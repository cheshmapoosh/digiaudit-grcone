package com.digiaudit.grcone.modules.masterdata.catalogimport.application;

import com.digiaudit.grcone.common.exception.ForbiddenException;
import com.digiaudit.grcone.common.persistence.ProfileScoped;
import com.digiaudit.grcone.common.security.ProfileContext;
import com.digiaudit.grcone.modules.basicdata.application.BasicDataService;
import com.digiaudit.grcone.modules.basicdata.domain.BasicDataKind;
import com.digiaudit.grcone.modules.basicdata.domain.BasicDataType;
import com.digiaudit.grcone.modules.masterdata.catalogimport.application.CatalogImportRows.Row;
import com.digiaudit.grcone.modules.masterdata.catalogimport.application.CatalogImportSchema.Family;
import com.digiaudit.grcone.modules.masterdata.catalogimport.application.CatalogImportSchema.Kind;
import com.digiaudit.grcone.modules.masterdata.objective.domain.ObjectiveKind;
import com.digiaudit.grcone.modules.masterdata.revision.application.MasterDataRevisionActorProvider;
import com.digiaudit.grcone.modules.masterdata.revision.application.MasterDataRevisionCoordinator;
import com.digiaudit.grcone.modules.masterdata.revision.application.RevisionExecutionContext;
import com.digiaudit.grcone.modules.masterdata.revision.application.RevisionMutationGuard;
import com.digiaudit.grcone.modules.masterdata.revision.application.RevisionOperation;
import com.digiaudit.grcone.modules.masterdata.revision.application.RevisionOperationResult;
import com.digiaudit.grcone.modules.masterdata.revision.application.RevisionRequest;
import com.digiaudit.grcone.modules.masterdata.revision.domain.RevisionContentResult;
import com.digiaudit.grcone.modules.masterdata.revision.domain.RevisionEntityType;
import com.digiaudit.grcone.modules.masterdata.security.MasterDataAuthorizationService;
import com.digiaudit.grcone.modules.masterdata.shared.application.workbook.MasterDataWorkbookReader;
import com.digiaudit.grcone.modules.masterdata.shared.application.workbook.MasterDataWorkbookReader.Issue;
import com.digiaudit.grcone.modules.masterdata.shared.application.workbook.MasterDataWorkbookReader.Parsed;
import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataMutationResult;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.digiaudit.grcone.modules.masterdata.catalogimport.application.CatalogImportRows.issue;
import static com.digiaudit.grcone.modules.masterdata.catalogimport.application.CatalogImportSchema.Column.CODE;
import static com.digiaudit.grcone.modules.masterdata.catalogimport.application.CatalogImportSchema.Column.ORGANIZATION_TYPE;
import static com.digiaudit.grcone.modules.masterdata.catalogimport.application.CatalogImportSchema.Column.PARENT_CODE;

@Service
public class CatalogImportService {
  private final EntityManager entities;
  private final MasterDataWorkbookReader reader;
  private final CatalogImportRows rows;
  private final BasicDataService basicData;
  private final MasterDataAuthorizationService authorization;
  private final MasterDataRevisionCoordinator revisions;
  private final MasterDataRevisionActorProvider actors;
  private final RevisionMutationGuard mutationGuard;
  private final Clock clock;

  public CatalogImportService(EntityManager entities, MasterDataWorkbookReader reader, CatalogImportRows rows,
      BasicDataService basicData, MasterDataAuthorizationService authorization, MasterDataRevisionCoordinator revisions,
      MasterDataRevisionActorProvider actors, RevisionMutationGuard mutationGuard,
      @Qualifier("masterDataRevisionClock") Clock clock) {
    this.entities = entities;
    this.reader = reader;
    this.rows = rows;
    this.basicData = basicData;
    this.authorization = authorization;
    this.revisions = revisions;
    this.actors = actors;
    this.mutationGuard = mutationGuard;
    this.clock = clock;
  }

  public record PreviewRow(int rowNumber, String kind, String code, String title, String parentCode,
      Integer parentRow, String existingParentTitle, String existingParentKind,
      LocalDate validFrom, LocalDate validTo, boolean valid) {}

  public record Preview(Family family, boolean valid, String fingerprint, List<PreviewRow> rows, List<Issue> issues) {}
  public record Result(UUID revisionId, int count) {}

  private record Key(RevisionEntityType storage, String code) {
    Key(Kind kind, String code) { this(kind.revisionType, code); }
  }

  private record Existing(UUID id, String title, MasterDataLifecycleStatus status, long version,
      Kind kind, UUID parentId) {}

  private record Ancestor(UUID parentId, MasterDataLifecycleStatus status, long version) {}

  private record Parent(Integer row, Existing existing) {}
  private record Plan(Preview preview, List<Row> ordered, Map<Integer, Parent> parents, Map<Integer, Integer> depths) {}
  private record NewRecord(Row row, UUID id, ProfileScoped entity) {}

  public static final class InvalidPreview extends RuntimeException {
    private final Preview preview;
    private final boolean stale;

    InvalidPreview(Preview preview, boolean stale) {
      this.preview = preview;
      this.stale = stale;
    }

    public Preview preview() { return preview; }
    public boolean stale() { return stale; }
  }

  public void requireAccess(Family family) {
    if (!authorization.canManage(family.area)) {
      throw new ForbiddenException("MASTER_DATA_ACCESS_DENIED", "error.security.forbidden",
          "Catalog import requires the catalog manage permission");
    }
  }

  @Transactional(readOnly = true)
  public byte[] template(Family family) {
    requireAccess(family);
    List<String> organizationTypes = family == Family.ORGANIZATION
        ? activeOrganizationTypes().stream().map(BasicDataType::name).toList() : List.of();
    return CatalogImportTemplate.write(family, organizationTypes);
  }

  @Transactional(readOnly = true)
  public Preview preview(Family family, MultipartFile file) {
    requireAccess(family);
    return plan(family, readWorkbook(family, file)).preview();
  }

  public Result commit(Family family, MultipartFile file, String fingerprint) {
    requireAccess(family);
    var parsed = readWorkbook(family, file);
    var request = RevisionRequest.central("Import " + family.name() + " catalog", "Excel catalog import", null);
    RevisionOperation execute = context -> {
      requireAccess(family);
      family.guards.forEach(key -> mutationGuard.requireHierarchyGuard(context, key));
      var plan = plan(family, parsed);
      if (!plan.preview().valid()) throw new InvalidPreview(plan.preview(), false);
      if (!plan.preview().fingerprint().equals(fingerprint)) throw new InvalidPreview(plan.preview(), true);
      return insert(context, plan);
    };
    var result = family.guards.isEmpty() ? revisions.execute(request, execute)
        : revisions.executeStructural(family.guards, request, execute);
    return new Result(result.primaryResult().revisionId(), result.revisionContents().size());
  }

  private Parsed readWorkbook(Family family, MultipartFile file) {
    var parsed = reader.read(file, family.workbookSchema());
    if (family != Family.CONTROL || !hasWrongHeaders(parsed)) return parsed;
    var legacy = reader.read(file, family.workbookSchema(family.legacyColumns()));
    return hasWrongHeaders(legacy) || legacy.digest().isEmpty() ? parsed : legacy;
  }

  private boolean hasWrongHeaders(Parsed parsed) {
    return parsed.issues().stream().anyMatch(issue -> "HEADERS".equals(issue.code()));
  }

  private Plan plan(Family family, Parsed parsed) {
    List<Issue> issues = new ArrayList<>(parsed.issues());
    // A mismatched schema provides no reliable column-to-field mapping.
    boolean wrongSchema = hasWrongHeaders(parsed);
    List<Row> input = wrongSchema || parsed.rows().isEmpty() ? List.of() : parseRows(family, parsed, issues);
    Map<Key, List<Row>> byKey = new LinkedHashMap<>();
    for (Row row : input) {
      if (row.kind() != null && lookupCode(row.code()))
        byKey.computeIfAbsent(new Key(row.kind(), row.code()), ignored -> new ArrayList<>()).add(row);
    }

    Map<Key, Existing> existing = lookup(family, input);
    for (var entry : byKey.entrySet()) {
      if (entry.getValue().size() > 1)
        entry.getValue().forEach(row -> issue(issues, row.number(), CODE, "DUPLICATE_FILE_CODE"));
      if (existing.containsKey(entry.getKey()))
        entry.getValue().forEach(row -> issue(issues, row.number(), CODE, "CODE_RESERVED"));
    }
    Map<Integer, Parent> parents = resolveParents(input, byKey, existing, issues);
    Map<UUID, Ancestor> ancestors = loadAncestors(parents);
    validateExistingPaths(parents, ancestors, issues);
    Set<Integer> invalid = new HashSet<>();
    issues.forEach(issue -> invalid.add(issue.rowNumber()));
    Map<Integer, Integer> depths = new HashMap<>();
    for (Row row : input) depth(row.number(), parents, depths, new LinkedHashSet<>(), invalid, issues);

    String fingerprint = issues.isEmpty() ? fingerprint(family, parsed, input, parents, ancestors) : null;
    List<PreviewRow> previewRows = input.stream().map(row -> {
      Parent parent = parents.get(row.number());
      Existing existingParent = parent == null ? null : parent.existing();
      return new PreviewRow(row.number(), row.kind() == null ? row.rawKind() : row.kind().name(), row.code(),
          row.title(), row.parentCode(), parent == null ? null : parent.row(),
          existingParent == null ? null : existingParent.title(), existingParent == null ? null : existingParent.kind().name(),
          row.validFrom(), row.validTo(), !invalid.contains(row.number()));
    }).toList();
    issues.sort(Comparator.comparingInt(Issue::rowNumber).thenComparing(Issue::field));
    var preview = new Preview(family, fingerprint != null, fingerprint, previewRows, List.copyOf(issues));
    List<Row> ordered = preview.valid()
        ? input.stream().sorted(Comparator.comparingInt(row -> depths.get(row.number()))).toList() : List.of();
    return new Plan(preview, ordered, parents, depths);
  }

  private List<Row> parseRows(Family family, Parsed parsed, List<Issue> issues) {
    Map<String, BasicDataType> types = new HashMap<>();
    if (family == Family.ORGANIZATION) {
      for (var type : activeOrganizationTypes()) types.put(type.name().toLowerCase(Locale.ROOT), type);
    }
    return parsed.rows().stream().map(row -> rows.parse(family, row, types, issues)).toList();
  }

  private List<BasicDataType> activeOrganizationTypes() {
    return basicData.list(BasicDataKind.ORGANIZATION_TYPE, false).stream()
        .filter(type -> "ACTIVE".equals(type.status())).toList();
  }

  private Map<Key, Existing> lookup(Family family, List<Row> input) {
    Map<Key, Existing> result = new HashMap<>();
    Set<RevisionEntityType> read = new HashSet<>();
    for (Kind kind : family.kinds) {
      if (!read.add(kind.revisionType)) continue;
      Set<String> codes = new HashSet<>();
      for (Row row : input) {
        if (row.kind() == null) continue;
        if (row.kind().revisionType == kind.revisionType && lookupCode(row.code())) codes.add(row.code());
        if (row.kind().parentKind() != null && row.kind().parentKind().revisionType == kind.revisionType
            && lookupCode(row.parentCode())) codes.add(row.parentCode());
      }
      if (codes.isEmpty()) continue;
      boolean objective = kind.revisionType == RevisionEntityType.OBJECTIVE;
      String parentField = parentField(kind);
      String extraFields = (objective ? ", e.kind" : "") + (parentField == null ? "" : ", e." + parentField);
      // Entity/attribute names are constants from the closed import schema.
      var matches = entities.createQuery("select e.id, e.code, e." + kind.titleProperty
          + ", e.status, e.version" + extraFields + " from " + kind.entityClass.getSimpleName()
          + " e where e.profileId = :profile and e.code in :codes", Object[].class)
          .setParameter("profile", ProfileContext.requireId()).setParameter("codes", codes).getResultList();
      for (Object[] match : matches) {
        Kind storedKind = objective ? Kind.valueOf(((ObjectiveKind) match[5]).name()) : kind;
        var found = new Existing((UUID) match[0], (String) match[2], (MasterDataLifecycleStatus) match[3],
            ((Number) match[4]).longValue(), storedKind,
            parentField == null ? null : (UUID) match[objective ? 6 : 5]);
        result.put(new Key(kind, (String) match[1]), found);
      }
    }
    return result;
  }

  private Map<Integer, Parent> resolveParents(List<Row> input, Map<Key, List<Row>> byKey,
      Map<Key, Existing> existing, List<Issue> issues) {
    Map<Integer, Parent> parents = new HashMap<>();
    Set<String> knownCodes = new HashSet<>();
    input.forEach(row -> knownCodes.add(row.code()));
    for (Row row : input) {
      Kind expected = row.kind() == null ? null : row.kind().parentKind();
      if (expected == null || !lookupCode(row.parentCode())) continue;
      Key key = new Key(expected, row.parentCode());
      List<Row> candidates = byKey.getOrDefault(key, List.of());
      Existing external = existing.get(key);
      if (candidates.size() > 1) {
        issue(issues, row.number(), PARENT_CODE, "PARENT_AMBIGUOUS");
      } else if (!candidates.isEmpty()) {
        Row parent = candidates.getFirst();
        if (parent.kind() != expected) issue(issues, row.number(), PARENT_CODE, "PARENT_KIND");
        else if (parent.number() == row.number()) issue(issues, row.number(), PARENT_CODE, "SELF_PARENT");
        else parents.put(row.number(), new Parent(parent.number(), null));
      } else if (external == null) {
        issue(issues, row.number(), PARENT_CODE, knownCodes.contains(row.parentCode()) ? "PARENT_KIND" : "PARENT_MISSING");
      } else if (external.kind() != expected || (expected == Kind.STRATEGY && external.parentId() != null)) {
        issue(issues, row.number(), PARENT_CODE, "PARENT_KIND");
      } else if (external.status() == MasterDataLifecycleStatus.DELETED) {
        issue(issues, row.number(), PARENT_CODE, "PARENT_DELETED");
      } else if (row.kind() == Kind.GLOBAL_CONTROL && external.status() != MasterDataLifecycleStatus.ACTIVE) {
        issue(issues, row.number(), PARENT_CODE, "PARENT_INACTIVE");
      } else {
        parents.put(row.number(), new Parent(null, external));
      }
    }
    return parents;
  }

  private boolean lookupCode(String code) {
    return !code.isBlank() && code.getBytes(StandardCharsets.UTF_8).length <= 64;
  }

  private String parentField(Kind kind) {
    return switch (kind) {
      case ORGANIZATION -> "parentOrganizationId";
      case PROCESS -> "parentProcessId";
      case STRATEGY, OBJECTIVE -> "parentObjectiveId";
      case CONTROL_GROUP, REGULATION_GROUP, POLICY_GROUP -> "parentGroupId";
      case GLOBAL_CONTROL_GROUP -> "parentId";
      case REGULATION -> "regulationGroupId";
      case RISK_CATEGORY -> "parentCategoryId";
      case ACCOUNT_GROUP -> "parentAccountGroupId";
      default -> null;
    };
  }

  private Map<UUID, Ancestor> loadAncestors(Map<Integer, Parent> parents) {
    Map<Kind, Set<UUID>> roots = new java.util.EnumMap<>(Kind.class);
    for (Parent parent : parents.values()) {
      Existing external = parent.existing();
      if (external != null && external.parentId() != null) {
        // A saved law has a group as its parent. The remaining chain is group-to-group.
        roots.computeIfAbsent(external.kind().parentKind(), ignored -> new HashSet<>()).add(external.parentId());
      }
    }
    Map<UUID, Ancestor> result = new HashMap<>();
    for (var chain : roots.entrySet()) {
      Kind kind = chain.getKey();
      String parentField = parentField(kind);
      Set<UUID> frontier = chain.getValue();
      Set<UUID> checked = new HashSet<>();
      while (!frontier.isEmpty()) {
        checked.addAll(frontier);
        var batch = entities.createQuery("select e.id, e." + parentField + ", e.status, e.version from "
            + kind.entityClass.getSimpleName() + " e where e.profileId = :profile and e.id in :ids", Object[].class)
            .setParameter("profile", ProfileContext.requireId()).setParameter("ids", frontier).getResultList();
        Set<UUID> next = new HashSet<>();
        for (Object[] found : batch) {
          UUID parentId = (UUID) found[1];
          result.put((UUID) found[0], new Ancestor(parentId, (MasterDataLifecycleStatus) found[2],
              ((Number) found[3]).longValue()));
          if (parentId != null && !checked.contains(parentId)) next.add(parentId);
        }
        frontier = next;
      }
    }
    return result;
  }

  private void validateExistingPaths(Map<Integer, Parent> parents, Map<UUID, Ancestor> ancestors, List<Issue> issues) {
    for (var entry : parents.entrySet()) {
      var external = entry.getValue().existing();
      if (external == null) continue;
      Set<UUID> seen = new HashSet<>();
      seen.add(external.id());
      UUID cursor = external.parentId();
      while (cursor != null) {
        if (!seen.add(cursor)) {
          issue(issues, entry.getKey(), PARENT_CODE, "CYCLE");
          break;
        }
        var ancestor = ancestors.get(cursor);
        if (ancestor == null || ancestor.status() == MasterDataLifecycleStatus.DELETED) {
          issue(issues, entry.getKey(), PARENT_CODE, "PARENT_ANCESTRY_INVALID");
          break;
        }
        cursor = ancestor.parentId();
      }
    }
  }

  private int depth(int number, Map<Integer, Parent> parents, Map<Integer, Integer> depths,
      Set<Integer> path, Set<Integer> invalid, List<Issue> issues) {
    Integer cached = depths.get(number);
    if (cached != null) return cached;
    if (!path.add(number)) {
      boolean inCycle = false;
      for (int item : path) {
        if (item == number) inCycle = true;
        if (inCycle) {
          issue(issues, item, PARENT_CODE, "CYCLE");
          invalid.add(item);
        }
      }
      return -1;
    }
    int result = 0;
    Parent parent = parents.get(number);
    if (parent != null && parent.row() != null) {
      int parentDepth = depth(parent.row(), parents, depths, path, invalid, issues);
      if (parentDepth < 0) {
        if (invalid.add(number)) issue(issues, number, PARENT_CODE, "PARENT_INVALID_ROW");
      } else result = parentDepth + 1;
    }
    path.remove(number);
    if (invalid.contains(number)) result = -1;
    depths.put(number, result);
    return result;
  }

  private String fingerprint(Family family, Parsed parsed, List<Row> input, Map<Integer, Parent> parents,
      Map<UUID, Ancestor> ancestors) {
    StringBuilder state = new StringBuilder(parsed.digest()).append(':').append(family)
        .append(':').append(ProfileContext.requireId());
    for (Row row : input) {
      Parent parent = parents.get(row.number());
      if (parent != null && parent.existing() != null) {
        var saved = parent.existing();
        state.append('|').append(saved.id()).append(':').append(saved.version()).append(':').append(saved.status());
      }
      var type = row.value(ORGANIZATION_TYPE, BasicDataType.class);
      if (type != null) state.append('|').append(type.id()).append(':').append(type.version());
    }
    ancestors.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> state
        .append('|').append(entry.getKey()).append(':').append(entry.getValue().version())
        .append(':').append(entry.getValue().status()));
    return MasterDataWorkbookReader.digest(state.toString().getBytes(StandardCharsets.UTF_8));
  }

  private RevisionOperationResult insert(RevisionExecutionContext context, Plan plan) {
    UUID actor = actors.currentActorId();
    Instant now = Instant.now(clock);
    Map<Integer, UUID> ids = new HashMap<>();
    List<NewRecord> records = new ArrayList<>();
    for (Row row : plan.ordered()) {
      var parent = plan.parents().get(row.number());
      UUID parentId = null;
      if (parent != null) parentId = parent.row() == null ? parent.existing().id() : ids.get(parent.row());
      UUID id = UUID.randomUUID();
      records.add(new NewRecord(row, id, rows.create(row, id, parentId, actor, now)));
      ids.put(row.number(), id);
    }

    int writtenDepth = 0;
    for (var record : records) {
      int depth = plan.depths().get(record.row().number());
      // Parent IDs are scalar fields. Flush each completed level before queuing its children.
      if (depth != writtenDepth) {
        entities.flush();
        writtenDepth = depth;
      }
      entities.persist(record.entity());
    }
    entities.flush();

    List<RevisionContentResult> contents = records.stream()
        .map(record -> rows.revision(context, record.row(), record.entity(), record.id())).toList();
    var first = contents.getFirst();
    return RevisionOperationResult.completed(context,
        new MasterDataMutationResult(first.entityId(), context.revisionId(), first.appliedEntityVersion()), contents);
  }
}

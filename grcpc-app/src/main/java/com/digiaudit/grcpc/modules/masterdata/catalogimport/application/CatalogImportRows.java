package com.digiaudit.grcpc.modules.masterdata.catalogimport.application;

import com.digiaudit.grcpc.common.exception.UnprocessableEntityException;
import com.digiaudit.grcpc.common.persistence.ProfileScoped;
import com.digiaudit.grcpc.modules.basicdata.application.BasicDataService;
import com.digiaudit.grcpc.modules.basicdata.domain.BasicDataType;
import com.digiaudit.grcpc.modules.masterdata.catalog.accountgroup.domain.entity.CentralAccountGroupEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.accountgroup.domain.enums.CentralAccountGroupImportance;
import com.digiaudit.grcpc.modules.masterdata.catalog.control.domain.entity.CentralControlEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.control.domain.entity.CentralControlGroupEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.control.domain.enums.*;
import com.digiaudit.grcpc.modules.masterdata.catalog.controlobjective.domain.entity.CentralControlObjectiveEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.policy.domain.CentralPolicyType;
import com.digiaudit.grcpc.modules.masterdata.catalog.policy.domain.PolicyCommunicationMethod;
import com.digiaudit.grcpc.modules.masterdata.catalog.policy.domain.entity.CentralPolicyEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.policy.domain.entity.CentralPolicyGroupEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.regulation.domain.entity.CentralRegulationEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.regulation.domain.entity.CentralRegulationGroupEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.regulation.domain.entity.CentralRegulationRequirementEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.risk.domain.entity.CentralRiskCategoryEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.risk.domain.entity.CentralRiskTemplateEntity;
import com.digiaudit.grcpc.modules.masterdata.catalog.risk.domain.enums.CentralRiskType;
import com.digiaudit.grcpc.modules.masterdata.catalog.shared.application.CatalogCommandSupport;
import com.digiaudit.grcpc.modules.masterdata.catalog.shared.domain.entity.CentralDefinitionEntity;
import com.digiaudit.grcpc.modules.masterdata.objective.domain.ObjectiveEntity;
import com.digiaudit.grcpc.modules.masterdata.objective.domain.ObjectiveKind;
import com.digiaudit.grcpc.modules.masterdata.process.domain.entity.CentralProcessEntity;
import com.digiaudit.grcpc.modules.masterdata.process.domain.entity.CentralSubprocessEntity;
import com.digiaudit.grcpc.modules.masterdata.revision.application.RevisionExecutionContext;
import com.digiaudit.grcpc.modules.masterdata.revision.domain.RevisionContentResult;
import com.digiaudit.grcpc.modules.masterdata.revision.domain.RevisionOperationType;
import com.digiaudit.grcpc.modules.masterdata.shared.application.workbook.MasterDataImportDates;
import com.digiaudit.grcpc.modules.masterdata.shared.application.workbook.MasterDataWorkbookReader.Issue;
import com.digiaudit.grcpc.modules.masterdata.shared.application.workbook.MasterDataWorkbookReader.RawRow;
import com.digiaudit.grcpc.modules.organization.domain.OrganizationType;
import com.digiaudit.grcpc.modules.organization.domain.entity.OrganizationEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

import static com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportSchema.Column.*;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportSchema.Column;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportSchema.Family;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportSchema.Kind;

@Component
class CatalogImportRows {
  private static final Set<Column> REQUIRED = EnumSet.of(
      ORGANIZATION_TYPE, RISK_TYPE, POLICY_TYPE, ACCOUNT_IMPORTANCE, REASONABLE_ASSURANCE);

  private final CatalogCommandSupport support;
  private final BasicDataService basicData;
  private final ObjectMapper json;

  CatalogImportRows(CatalogCommandSupport support, BasicDataService basicData, ObjectMapper json) {
    this.support = support;
    this.basicData = basicData;
    this.json = json;
  }

  record Row(int number, Kind kind, String rawKind, String code, String title, String parentCode,
      String description, LocalDate validFrom, LocalDate validTo, Map<Column, Object> values) {

    <T> T value(Column field, Class<T> type) {
      return type.cast(values.get(field));
    }

    String text(Column field) {
      return value(field, String.class);
    }
  }

  Row parse(Family family, RawRow raw, Map<String, BasicDataType> organizationTypes, List<Issue> issues) {
    Map<Column, String> cells = new EnumMap<>(Column.class);
    for (int i = 0; i < family.columns.size(); i++) cells.put(family.columns.get(i), raw.cells().get(i));

    int number = raw.rowNumber();
    String rawKind = cells.getOrDefault(KIND, "");
    Kind kind = family.kind(rawKind);
    if (kind == null) issue(issues, number, KIND, rawKind.isBlank() ? "REQUIRED" : "INVALID_KIND");
    String code = normalizeCode(cells.get(CODE), CODE, number, issues);
    String title = cells.get(TITLE);
    try {
      title = support.normalizeTitle(title);
    } catch (UnprocessableEntityException error) {
      issue(issues, number, TITLE, title.isBlank() ? "REQUIRED" : "TOO_LONG");
    }
    String parentCode = cells.getOrDefault(PARENT_CODE, "");
    if (!parentCode.isBlank()) parentCode = normalizeCode(parentCode, PARENT_CODE, number, issues);
    if (kind != null) {
      if (kind.requiresParent() && parentCode.isBlank()) issue(issues, number, PARENT_CODE, "PARENT_REQUIRED");
      if (kind.parentKind() == null && !parentCode.isBlank()) issue(issues, number, PARENT_CODE, "PARENT_FORBIDDEN");
    }

    LocalDate from = date(cells.get(VALID_FROM), VALID_FROM, number, issues);
    LocalDate to = date(cells.get(VALID_TO), VALID_TO, number, issues);
    try {
      support.validateValidity(from, to);
    } catch (UnprocessableEntityException error) {
      issue(issues, number, VALID_TO, "DATE_RANGE");
    }

    Map<Column, Object> values = new EnumMap<>(Column.class);
    if (kind != null) {
      for (Column field : family.columns) {
        if (family.kinds.stream().noneMatch(candidate -> candidate.extra.contains(field))) continue;
        String value = cells.get(field);
        if (!kind.extra.contains(field)) {
          if (!value.isBlank()) issue(issues, number, field, "NOT_APPLICABLE");
        } else if (value.isBlank()) {
          if (REQUIRED.contains(field)) issue(issues, number, field, "REQUIRED");
        } else {
          Object parsed = parseExtra(field, value, number, organizationTypes, issues);
          if (parsed != null) values.put(field, parsed);
        }
      }
      if (kind == Kind.CONTROL) {
        Object trigger = values.get(TRIGGER_TYPE);
        if (values.containsKey(EVENT_DESCRIPTION) && trigger != CentralControlTriggerType.EVENT)
          issue(issues, number, EVENT_DESCRIPTION, "EVENT_TRIGGER_REQUIRED");
        if (values.containsKey(OPERATION_FREQUENCY) && trigger != CentralControlTriggerType.DATE)
          issue(issues, number, OPERATION_FREQUENCY, "DATE_TRIGGER_REQUIRED");
      }
    }
    return new Row(number, kind, rawKind, code, title, parentCode, support.normalizeDescription(cells.get(DESCRIPTION)),
        from, to, Map.copyOf(values));
  }

  private String normalizeCode(String value, Column field, int number, List<Issue> issues) {
    try {
      return support.normalizeCode(value);
    } catch (UnprocessableEntityException error) {
      issue(issues, number, field, value.isBlank() ? "REQUIRED" : "TOO_LONG");
      return value;
    }
  }

  private Object parseExtra(Column field, String value, int number,
      Map<String, BasicDataType> organizationTypes, List<Issue> issues) {
    if (field == ORGANIZATION_TYPE) {
      var type = organizationTypes.get(value.toLowerCase(Locale.ROOT));
      if (type == null) issue(issues, number, field, "ORGANIZATION_TYPE_INVALID");
      return type;
    }
    if (field == NEXT_REVIEW_DATE) return date(value, field, number, issues);
    if (field == TO_BE_TESTED || field == REASONABLE_ASSURANCE) {
      return switch (MasterDataImportDates.englishDigits(value).toLowerCase(Locale.ROOT)) {
        case "true", "1", "yes", "بله" -> Boolean.TRUE;
        case "false", "0", "no", "خیر" -> Boolean.FALSE;
        default -> {
          issue(issues, number, field, "INVALID_BOOLEAN");
          yield null;
        }
      };
    }
    if (field == CONTROL_RELEVANCE) {
      Set<CentralControlRelevance> choices = EnumSet.noneOf(CentralControlRelevance.class);
      for (String token : value.split("[,،;؛]", -1)) {
        try {
          if (!choices.add(CentralControlRelevance.valueOf(token.trim().toUpperCase(Locale.ROOT)))) {
            issue(issues, number, field, "DUPLICATE_OPTION");
            return null;
          }
        } catch (IllegalArgumentException error) {
          issue(issues, number, field, "INVALID_OPTION");
          return null;
        }
      }
      return choices;
    }
    if (field.options != null) {
      var option = Arrays.stream(field.options.getEnumConstants())
          .filter(choice -> choice.name().equalsIgnoreCase(value)).findFirst().orElse(null);
      if (option == null) issue(issues, number, field, "INVALID_OPTION");
      return option;
    }
    String normalized = field == OBJECTIVE_TYPE ? value.toUpperCase(Locale.ROOT) : value;
    boolean tooLong = switch (field) {
      case LOCATION, OBJECTIVE_CLASS, RESPONSIBLE_ORGANIZATION -> normalized.length() > 255;
      case OBJECTIVE_TYPE -> normalized.getBytes(StandardCharsets.UTF_8).length > 64;
      case EVENT_DESCRIPTION -> normalized.length() > 1000;
      default -> false;
    };
    if (tooLong) issue(issues, number, field, "TOO_LONG");
    return normalized;
  }

  private LocalDate date(String value, Column field, int number, List<Issue> issues) {
    try {
      return MasterDataImportDates.parse(value);
    } catch (DateTimeException error) {
      issue(issues, number, field, "INVALID_DATE");
      return null;
    }
  }

  static void issue(List<Issue> issues, int number, Column field, String code) {
    issues.add(new Issue(number, field.name(), code));
  }

  ProfileScoped create(Row row, UUID id, UUID parent, UUID actor, Instant now) {
    String code = row.code();
    String title = row.title();
    String description = row.description();
    var from = row.validFrom();
    var to = row.validTo();
    return switch (row.kind()) {
      case ORGANIZATION -> {
        UUID typeId = row.value(ORGANIZATION_TYPE, BasicDataType.class).id();
        var organization = OrganizationEntity.create(id, code, title,
            OrganizationType.fromWireValue(basicData.legacyOrganizationCode(typeId)), parent, row.text(LOCATION),
            description, from, to, actor, now);
        organization.setOrganizationTypeId(typeId);
        yield organization;
      }
      case PROCESS -> CentralProcessEntity.create(id, code, title, parent, description, 0, from, to, actor, now);
      case SUBPROCESS -> CentralSubprocessEntity.create(id, code, title, parent, description, 0, from, to, actor, now);
      case STRATEGY, OBJECTIVE -> new ObjectiveEntity(id, code, title, description, row.text(OBJECTIVE_TYPE), parent,
          from, to, actor, now, ObjectiveKind.valueOf(row.kind().name()));
      case CONTROL_OBJECTIVE -> CentralControlObjectiveEntity.create(id, code, title, description,
          row.text(OBJECTIVE_CLASS), from, to, actor, now);
      case CONTROL_GROUP -> CentralControlGroupEntity.create(id, code, title, parent, description, 0, from, to, actor, now);
      case CONTROL -> createControl(row, id, parent, actor, now);
      case RISK_CATEGORY -> CentralRiskCategoryEntity.create(id, code, title, parent, description, 0, from, to, actor, now);
      case RISK_TEMPLATE -> CentralRiskTemplateEntity.create(id, code, title, parent, row.value(RISK_TYPE, CentralRiskType.class),
          description, 0, from, to, actor, now);
      case REGULATION_GROUP ->
          CentralRegulationGroupEntity.create(id, code, title, parent, description, 0, from, to, actor, now);
      case REGULATION -> CentralRegulationEntity.create(id, code, title, parent, description, 0, from, to, actor, now);
      case REGULATION_REQUIREMENT ->
          CentralRegulationRequirementEntity.create(id, code, title, parent, description, 0, from, to, actor, now);
      case POLICY_GROUP -> CentralPolicyGroupEntity.create(id, code, title, parent, description, 0, from, to, actor, now);
      case POLICY -> CentralPolicyEntity.create(id, code, title, parent, row.value(POLICY_TYPE, CentralPolicyType.class),
          row.text(RESPONSIBLE_ORGANIZATION), row.value(COMMUNICATION_METHOD, PolicyCommunicationMethod.class),
          row.value(NEXT_REVIEW_DATE, LocalDate.class), row.text(POLICY_OBJECTIVE), row.text(CONTENT), description,
          0, from, to, actor, now);
      case ACCOUNT_GROUP -> CentralAccountGroupEntity.create(id, code, title, parent,
          row.value(ACCOUNT_IMPORTANCE, CentralAccountGroupImportance.class), row.value(REASONABLE_ASSURANCE, Boolean.class),
          description, 0, from, to, actor, now);
    };
  }

  private CentralControlEntity createControl(Row row, UUID id, UUID parent, UUID actor, Instant now) {
    Set<CentralControlRelevance> relevance = EnumSet.noneOf(CentralControlRelevance.class);
    if (row.values().get(CONTROL_RELEVANCE) instanceof Set<?> options)
      for (Object option : options) relevance.add(CentralControlRelevance.class.cast(option));
    return CentralControlEntity.create(id, row.code(), row.title(), row.description(), parent,
        row.value(CONTROL_CLASS, CentralControlClass.class), row.value(CONTROL_IMPORTANCE, CentralControlImportance.class),
        row.value(CONTROL_RISK, CentralControlRisk.class), row.value(AUTOMATION_TYPE, CentralControlAutomationType.class),
        row.value(CONTROL_PURPOSE, CentralControlPurpose.class), row.value(NATURE, CentralControlNature.class), relevance,
        row.value(TRIGGER_TYPE, CentralControlTriggerType.class), row.text(EVENT_DESCRIPTION),
        row.value(OPERATION_FREQUENCY, CentralControlOperationFrequency.class), row.value(TO_BE_TESTED, Boolean.class),
        row.value(TEST_AUTOMATION_TYPE, CentralControlTestAutomationType.class),
        row.value(TESTING_TECHNIQUE, CentralControlTestingTechnique.class),
        row.value(EVIDENCE_LEVEL, CentralControlEvidenceLevel.class), row.validFrom(), row.validTo(), actor, now);
  }

  RevisionContentResult revision(RevisionExecutionContext context, Row row, ProfileScoped entity, UUID id) {
    Map<String, Object> fields = new LinkedHashMap<>();
    if (entity instanceof CentralDefinitionEntity definition) {
      switch (definition) {
        case ObjectiveEntity e -> {
          fields.put("kind", e.getKind());
          fields.put("objectiveType", e.getObjectiveType());
          fields.put("parentObjectiveId", e.getParentObjectiveId());
        }
        case CentralControlObjectiveEntity e -> fields.put("objectiveClass", e.getObjectiveClass());
        case CentralControlGroupEntity e -> {
          fields.put("parentGroupId", e.getParentGroupId());
          fields.put("sortOrder", e.getSortOrder());
        }
        case CentralControlEntity e -> controlFields(e, fields);
        case CentralRiskCategoryEntity e -> {
          fields.put("parentCategoryId", e.getParentCategoryId());
          fields.put("sortOrder", e.getSortOrder());
        }
        case CentralRiskTemplateEntity e -> {
          fields.put("riskCategoryId", e.getRiskCategoryId());
          fields.put("riskType", e.getRiskType());
          fields.put("sortOrder", e.getSortOrder());
        }
        case CentralRegulationGroupEntity e -> {
          fields.put("parentGroupId", e.getParentGroupId());
          fields.put("sortOrder", e.getSortOrder());
        }
        case CentralRegulationEntity e -> {
          fields.put("regulationGroupId", e.getRegulationGroupId());
          fields.put("sortOrder", e.getSortOrder());
        }
        case CentralRegulationRequirementEntity e -> {
          fields.put("regulationId", e.getRegulationId());
          fields.put("sortOrder", e.getSortOrder());
        }
        case CentralPolicyGroupEntity e -> {
          fields.put("parentGroupId", e.getParentGroupId());
          fields.put("sortOrder", e.getSortOrder());
        }
        case CentralPolicyEntity e -> {
          fields.put("policyGroupId", e.getPolicyGroupId());
          fields.put("policyType", e.getPolicyType());
          fields.put("responsibleOrganization", e.getResponsibleOrganization());
          fields.put("communicationMethod", e.getCommunicationMethod());
          fields.put("nextReviewDate", e.getNextReviewDate());
          fields.put("objective", e.getObjective());
          fields.put("content", e.getContent());
          fields.put("sortOrder", e.getSortOrder());
        }
        case CentralAccountGroupEntity e -> {
          fields.put("parentAccountGroupId", e.getParentAccountGroupId());
          fields.put("importance", e.getImportance());
          fields.put("reasonableAssurance", e.isReasonableAssurance());
          fields.put("sortOrder", e.getSortOrder());
        }
        default -> throw new IllegalStateException("Unsupported catalog import entity");
      }
      return support.completed(context, definition, row.kind().revisionType, RevisionOperationType.CREATE,
          null, null, fields).contentResults().getFirst();
    }
    var snapshot = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(entity);
    if (entity instanceof OrganizationEntity e) {
      snapshot.put("organizationType", e.getOrganizationType().wireValue());
      snapshot.put("organizationTypeName", row.value(ORGANIZATION_TYPE, BasicDataType.class).name());
      snapshot.putArray("ownerAssignments");
    }
    return RevisionContentResult.completed(row.kind().revisionType, id, RevisionOperationType.CREATE, null, null,
        snapshot, 0L, json.valueToTree(Map.of("validated", true)));
  }

  private void controlFields(CentralControlEntity e, Map<String, Object> fields) {
    fields.put("controlGroupId", e.getControlGroupId());
    fields.put("controlClass", e.getControlClass());
    fields.put("importance", e.getImportance());
    fields.put("controlRisk", e.getControlRisk());
    fields.put("automationType", e.getAutomationType());
    fields.put("controlPurpose", e.getControlPurpose());
    fields.put("nature", e.getNature());
    fields.put("controlRelevance", e.getControlRelevance());
    fields.put("triggerType", e.getTriggerType());
    fields.put("eventDescription", e.getEventDescription());
    fields.put("operationFrequency", e.getOperationFrequency());
    fields.put("toBeTested", e.getToBeTested());
    fields.put("testAutomationType", e.getTestAutomationType());
    fields.put("testingTechnique", e.getTestingTechnique());
    fields.put("evidenceLevel", e.getEvidenceLevel());
  }
}

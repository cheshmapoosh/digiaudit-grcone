package com.digiaudit.grcpc.modules.masterdata.catalogimport.application;

import com.digiaudit.grcpc.common.persistence.ProfileScoped;
import com.digiaudit.grcpc.modules.masterdata.catalog.accountgroup.domain.entity.CentralAccountGroupEntity;
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
import com.digiaudit.grcpc.modules.masterdata.catalog.accountgroup.domain.enums.CentralAccountGroupImportance;
import com.digiaudit.grcpc.modules.masterdata.objective.domain.ObjectiveEntity;
import com.digiaudit.grcpc.modules.masterdata.process.domain.entity.CentralProcessEntity;
import com.digiaudit.grcpc.modules.masterdata.process.domain.entity.CentralSubprocessEntity;
import com.digiaudit.grcpc.modules.masterdata.revision.domain.RevisionEntityType;
import com.digiaudit.grcpc.modules.masterdata.shared.application.workbook.MasterDataWorkbookReader;
import com.digiaudit.grcpc.modules.masterdata.shared.domain.MasterDataHierarchyKey;
import com.digiaudit.grcpc.modules.organization.domain.entity.OrganizationEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CatalogImportSchema {
  static final String SHEET = "داده‌ها";

  private CatalogImportSchema() {}

  public enum Family {
    ORGANIZATION("ORGANIZATION", keys(MasterDataHierarchyKey.ORGANIZATION), Kind.ORGANIZATION),
    PROCESS("PROCESS", keys(MasterDataHierarchyKey.PROCESS), Kind.PROCESS, Kind.SUBPROCESS),
    OBJECTIVE("REFERENCE", keys(MasterDataHierarchyKey.OBJECTIVE, MasterDataHierarchyKey.ORGANIZATION),
        Kind.STRATEGY, Kind.OBJECTIVE),
    CONTROL_OBJECTIVE("CONTROL", keys(), Kind.CONTROL_OBJECTIVE),
    CONTROL("CONTROL", keys(MasterDataHierarchyKey.CONTROL), Kind.CONTROL_GROUP, Kind.CONTROL),
    RISK("RISK", keys(MasterDataHierarchyKey.RISK), Kind.RISK_CATEGORY, Kind.RISK_TEMPLATE),
    REGULATION("GOVERNANCE", keys(MasterDataHierarchyKey.REGULATION),
        Kind.REGULATION_GROUP, Kind.REGULATION, Kind.REGULATION_REQUIREMENT),
    POLICY("GOVERNANCE", keys(MasterDataHierarchyKey.POLICY), Kind.POLICY_GROUP, Kind.POLICY),
    ACCOUNT_GROUP("REFERENCE", keys(MasterDataHierarchyKey.ACCOUNT_GROUP), Kind.ACCOUNT_GROUP);

    final String area;
    final List<MasterDataHierarchyKey> guards;
    final List<Kind> kinds;
    final List<Column> columns;

    Family(String area, List<MasterDataHierarchyKey> guards, Kind... kinds) {
      this.area = area;
      this.guards = guards;
      this.kinds = List.of(kinds);
      List<Column> ordered = new ArrayList<>();
      if (kinds[0] != Kind.CONTROL_OBJECTIVE) ordered.add(Column.KIND);
      ordered.addAll(List.of(Column.CODE, Column.TITLE));
      if (kinds[0] != Kind.CONTROL_OBJECTIVE) ordered.add(Column.PARENT_CODE);
      ordered.addAll(List.of(Column.DESCRIPTION, Column.VALID_FROM, Column.VALID_TO));
      for (Kind kind : kinds) ordered.addAll(kind.extra);
      this.columns = List.copyOf(ordered);
    }

    Kind kind(String value) {
      if (this == CONTROL_OBJECTIVE) return Kind.CONTROL_OBJECTIVE;
      return kinds.stream().filter(kind -> kind.fa.equals(value) || kind.name().equalsIgnoreCase(value)).findFirst().orElse(null);
    }

    MasterDataWorkbookReader.Schema workbookSchema() {
      return new MasterDataWorkbookReader.Schema(SHEET,
          List.of(columns.stream().map(c -> c.fa).toList(), columns.stream().map(c -> c.en).toList()),
          columns.stream().map(Enum::name).toList(), Set.of("CODE", "PARENT_CODE"),
          Set.of("REASONABLE_ASSURANCE", "TO_BE_TESTED"));
    }

    public String fileName() {
      return name().toLowerCase(Locale.ROOT).replace('_', '-') + "-template.xlsx";
    }

    private static List<MasterDataHierarchyKey> keys(MasterDataHierarchyKey... keys) {
      return List.of(keys);
    }
  }

  enum Kind {
    ORGANIZATION("سازمان", OrganizationEntity.class, RevisionEntityType.ORGANIZATION, "name",
        Column.ORGANIZATION_TYPE, Column.LOCATION),
    PROCESS("فرآیند", CentralProcessEntity.class, RevisionEntityType.CENTRAL_PROCESS, "title"),
    SUBPROCESS("زیرفرآیند", CentralSubprocessEntity.class, RevisionEntityType.CENTRAL_SUBPROCESS, "title"),
    STRATEGY("استراتژی", ObjectiveEntity.class, RevisionEntityType.OBJECTIVE, "title"),
    OBJECTIVE("هدف", ObjectiveEntity.class, RevisionEntityType.OBJECTIVE, "title", Column.OBJECTIVE_TYPE),
    CONTROL_OBJECTIVE("هدف کنترلی", CentralControlObjectiveEntity.class, RevisionEntityType.CENTRAL_CONTROL_OBJECTIVE,
        "title", Column.OBJECTIVE_CLASS),
    CONTROL_GROUP("گروه کنترل", CentralControlGroupEntity.class, RevisionEntityType.CENTRAL_CONTROL_GROUP, "title"),
    CONTROL("کنترل", CentralControlEntity.class, RevisionEntityType.CENTRAL_CONTROL, "title",
        Column.CONTROL_CLASS, Column.CONTROL_IMPORTANCE, Column.CONTROL_RISK, Column.AUTOMATION_TYPE,
        Column.CONTROL_PURPOSE, Column.NATURE, Column.CONTROL_RELEVANCE, Column.TRIGGER_TYPE,
        Column.EVENT_DESCRIPTION, Column.OPERATION_FREQUENCY, Column.TO_BE_TESTED,
        Column.TEST_AUTOMATION_TYPE, Column.TESTING_TECHNIQUE, Column.EVIDENCE_LEVEL),
    RISK_CATEGORY("دسته ریسک", CentralRiskCategoryEntity.class, RevisionEntityType.CENTRAL_RISK_CATEGORY, "title"),
    RISK_TEMPLATE("الگوی ریسک", CentralRiskTemplateEntity.class, RevisionEntityType.CENTRAL_RISK_TEMPLATE,
        "title", Column.RISK_TYPE),
    REGULATION_GROUP("گروه قانون", CentralRegulationGroupEntity.class, RevisionEntityType.CENTRAL_REGULATION_GROUP, "title"),
    REGULATION("قانون", CentralRegulationEntity.class, RevisionEntityType.CENTRAL_REGULATION, "title"),
    REGULATION_REQUIREMENT("الزام", CentralRegulationRequirementEntity.class,
        RevisionEntityType.CENTRAL_REGULATION_REQUIREMENT, "title"),
    POLICY_GROUP("گروه سیاست", CentralPolicyGroupEntity.class, RevisionEntityType.CENTRAL_POLICY_GROUP, "title"),
    POLICY("سیاست", CentralPolicyEntity.class, RevisionEntityType.CENTRAL_POLICY, "title",
        Column.POLICY_TYPE, Column.RESPONSIBLE_ORGANIZATION, Column.COMMUNICATION_METHOD,
        Column.NEXT_REVIEW_DATE, Column.POLICY_OBJECTIVE, Column.CONTENT),
    ACCOUNT_GROUP("گروه حساب", CentralAccountGroupEntity.class, RevisionEntityType.CENTRAL_ACCOUNT_GROUP,
        "title", Column.ACCOUNT_IMPORTANCE, Column.REASONABLE_ASSURANCE);

    final String fa;
    final Class<? extends ProfileScoped> entityClass;
    final RevisionEntityType revisionType;
    final String titleProperty;
    final List<Column> extra;

    Kind(String fa, Class<? extends ProfileScoped> entityClass, RevisionEntityType revisionType,
        String titleProperty, Column... extra) {
      this.fa = fa;
      this.entityClass = entityClass;
      this.revisionType = revisionType;
      this.titleProperty = titleProperty;
      this.extra = List.of(extra);
    }

    Kind parentKind() {
      return switch (this) {
        case ORGANIZATION, PROCESS, CONTROL_GROUP, RISK_CATEGORY, REGULATION_GROUP, POLICY_GROUP, ACCOUNT_GROUP -> this;
        case SUBPROCESS -> PROCESS;
        case OBJECTIVE -> STRATEGY;
        case CONTROL -> CONTROL_GROUP;
        case RISK_TEMPLATE -> RISK_CATEGORY;
        case REGULATION -> REGULATION_GROUP;
        case REGULATION_REQUIREMENT -> REGULATION;
        case POLICY -> POLICY_GROUP;
        case STRATEGY, CONTROL_OBJECTIVE -> null;
      };
    }

    boolean requiresParent() {
      var parent = parentKind();
      return parent != null && parent != this;
    }
  }

  enum Column {
    KIND("نوع رکورد", "Record type"),
    CODE("کد", "Code"),
    TITLE("عنوان", "Title"),
    PARENT_CODE("کد والد", "Parent code"),
    DESCRIPTION("توضیحات", "Description"),
    VALID_FROM("تاریخ اعتبار از", "Valid from"),
    VALID_TO("تاریخ اعتبار تا", "Valid to"),
    ORGANIZATION_TYPE("نوع سازمان", "Organization type"),
    LOCATION("محل سازمان", "Location"),
    OBJECTIVE_TYPE("نوع هدف", "Objective type"),
    OBJECTIVE_CLASS("طبقه هدف کنترلی", "Control objective class"),
    CONTROL_CLASS("طبقه کنترل", "Control class", CentralControlClass.class),
    CONTROL_IMPORTANCE("اهمیت", "Importance", CentralControlImportance.class),
    CONTROL_RISK("ریسک کنترل", "Control risk", CentralControlRisk.class),
    AUTOMATION_TYPE("نوع خودکارسازی", "Automation type", CentralControlAutomationType.class),
    CONTROL_PURPOSE("هدف کنترل", "Control purpose", CentralControlPurpose.class),
    NATURE("ماهیت", "Nature", CentralControlNature.class),
    CONTROL_RELEVANCE("حوزه‌های کنترلی", "Control relevance", CentralControlRelevance.class),
    TRIGGER_TYPE("محرک اجرا", "Trigger type", CentralControlTriggerType.class),
    EVENT_DESCRIPTION("شرح رخداد", "Event description"),
    OPERATION_FREQUENCY("تناوب اجرا", "Operation frequency", CentralControlOperationFrequency.class),
    TO_BE_TESTED("آزمون شود", "To be tested"),
    TEST_AUTOMATION_TYPE("خودکارسازی آزمون", "Test automation", CentralControlTestAutomationType.class),
    TESTING_TECHNIQUE("روش آزمون", "Testing technique", CentralControlTestingTechnique.class),
    EVIDENCE_LEVEL("سطح شواهد", "Evidence level", CentralControlEvidenceLevel.class),
    RISK_TYPE("نوع ریسک", "Risk type", CentralRiskType.class),
    POLICY_TYPE("نوع سیاست", "Policy type", CentralPolicyType.class),
    RESPONSIBLE_ORGANIZATION("متولی سیاست", "Responsible organization"),
    COMMUNICATION_METHOD("روش ابلاغ", "Communication method", PolicyCommunicationMethod.class),
    NEXT_REVIEW_DATE("تاریخ بازبینی بعدی", "Next review date"),
    POLICY_OBJECTIVE("هدف سیاست", "Policy objective"),
    CONTENT("محتوا", "Content"),
    ACCOUNT_IMPORTANCE("اهمیت", "Importance", CentralAccountGroupImportance.class),
    REASONABLE_ASSURANCE("اطمینان معقول", "Reasonable assurance");

    final String fa;
    final String en;
    final Class<? extends Enum<?>> options;

    Column(String fa, String en) {
      this(fa, en, null);
    }

    Column(String fa, String en, Class<? extends Enum<?>> options) {
      this.fa = fa;
      this.en = en;
      this.options = options;
    }
  }
}

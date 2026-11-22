package com.digiaudit.grcone.runtime;

import com.digiaudit.grcone.modules.masterdata.assignment.accountgrouprisk.domain.entity.CentralAccountGroupRiskAssignmentEntity;
import com.digiaudit.grcone.modules.masterdata.assignment.riskcontrol.domain.entity.CentralRiskControlAssignmentEntity;
import com.digiaudit.grcone.modules.masterdata.assignment.riskcontrolobjective.domain.entity.CentralRiskControlObjectiveAssignmentEntity;
import com.digiaudit.grcone.modules.masterdata.assignment.controlcontrolobjective.domain.entity.CentralControlControlObjectiveAssignmentEntity;
import com.digiaudit.grcone.modules.masterdata.assignment.requirementcontrol.domain.entity.CentralRequirementControlAssignmentEntity;
import com.digiaudit.grcone.common.persistence.AuditableEntity;
import com.digiaudit.grcone.common.persistence.BooleanNumberConverter;
import com.digiaudit.grcone.modules.audit.domain.entity.AuditLogEntity;
import com.digiaudit.grcone.modules.document.infrastructure.persistence.DocumentEntity;
import com.digiaudit.grcone.modules.document.infrastructure.persistence.DocumentLifecycleStatusConverter;
import com.digiaudit.grcone.modules.document.infrastructure.persistence.DocumentLinkEntity;
import com.digiaudit.grcone.modules.document.infrastructure.persistence.DocumentLinkTargetTypeConverter;
import com.digiaudit.grcone.modules.document.infrastructure.persistence.DocumentTempUploadEntity;
import com.digiaudit.grcone.modules.document.infrastructure.persistence.DocumentVersionEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.accountgroup.domain.entity.CentralAccountGroupEntity;
import com.digiaudit.grcone.modules.masterdata.globalcontrol.domain.ControlGroupEntity;
import com.digiaudit.grcone.modules.masterdata.globalcontrol.domain.GlobalControlEntity;
import com.digiaudit.grcone.modules.masterdata.globalcontrol.domain.GlobalControlRegulationEntity;
import com.digiaudit.grcone.modules.masterdata.objective.domain.ObjectiveEntity;
import com.digiaudit.grcone.modules.masterdata.objective.domain.OrganizationObjectiveEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.control.domain.converter.CentralControlRelevanceConverter;
import com.digiaudit.grcone.modules.masterdata.catalog.control.domain.entity.CentralControlEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.control.domain.entity.CentralControlGroupEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.controlobjective.domain.entity.CentralControlObjectiveEntity;
import com.digiaudit.grcone.modules.masterdata.classification.controlaccountgroup.domain.entity.CentralControlAccountGroupEntity;
import com.digiaudit.grcone.modules.masterdata.classification.controlobjectiveaccountgroup.domain.entity.CentralControlObjectiveAccountGroupEntity;
import com.digiaudit.grcone.modules.masterdata.coverage.controlcontrolobjective.domain.entity.CentralSubprocessControlControlObjectiveCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.coverage.requirementcontrol.domain.entity.CentralSubprocessRequirementControlCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.coverage.riskcontrol.domain.entity.CentralSubprocessRiskControlCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.coverage.riskcontrolobjective.domain.entity.CentralSubprocessRiskControlObjectiveCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.policy.domain.entity.CentralPolicyEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.policy.domain.entity.CentralPolicyGroupEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.policy.domain.entity.CentralPolicySubprocessScopeEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.policy.domain.entity.CentralPolicyOrganizationScopeEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.policy.domain.entity.CentralPolicyControlScopeEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.policy.domain.entity.CentralPolicyRequirementScopeEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.regulation.domain.entity.CentralRegulationEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.regulation.domain.entity.CentralRegulationGroupEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.regulation.domain.entity.CentralRegulationRequirementEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.risk.domain.entity.CentralRiskCategoryEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.risk.domain.entity.CentralRiskTemplateEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.shared.domain.entity.CentralDefinitionEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalOrganizationSubprocessScopeEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalSubprocessControlScopeEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalSubprocessRiskScopeEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalSubprocessControlObjectiveScopeEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalSubprocessRequirementScopeEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalSubprocessRiskControlCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalSubprocessRiskControlObjectiveCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalSubprocessControlControlObjectiveCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalSubprocessRequirementControlCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalPolicyOrganizationScopeEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalPolicySubprocessScopeEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalPolicyControlScopeEntity;
import com.digiaudit.grcone.modules.masterdata.local.domain.entity.LocalPolicyRequirementScopeEntity;
import com.digiaudit.grcone.modules.masterdata.process.domain.entity.CentralProcessEntity;
import com.digiaudit.grcone.modules.masterdata.process.domain.entity.CentralSubprocessEntity;
import com.digiaudit.grcone.modules.masterdata.revision.infrastructure.persistence.JsonNodeClobConverter;
import com.digiaudit.grcone.modules.masterdata.revision.infrastructure.persistence.MasterDataRevisionContentEntity;
import com.digiaudit.grcone.modules.masterdata.revision.infrastructure.persistence.MasterDataRevisionEntity;
import com.digiaudit.grcone.modules.masterdata.revision.infrastructure.persistence.RevisionEntityTypeConverter;
import com.digiaudit.grcone.modules.masterdata.scope.control.domain.entity.CentralSubprocessControlScopeEntity;
import com.digiaudit.grcone.modules.masterdata.scope.controlobjective.domain.entity.CentralSubprocessControlObjectiveScopeEntity;
import com.digiaudit.grcone.modules.masterdata.scope.risk.domain.entity.CentralSubprocessRiskScopeEntity;
import com.digiaudit.grcone.modules.masterdata.scope.requirement.domain.entity.CentralSubprocessRequirementScopeEntity;
import com.digiaudit.grcone.modules.masterdata.shared.infrastructure.persistence.MasterDataHierarchyGuardEntity;
import com.digiaudit.grcone.modules.masterdata.shared.infrastructure.persistence.MasterDataLifecycleStatusConverter;
import com.digiaudit.grcone.modules.organization.domain.entity.OrganizationEntity;
import com.digiaudit.grcone.modules.organization.infrastructure.persistence.OrganizationTypeConverter;
import com.digiaudit.grcone.modules.securityacl.domain.entity.ResourceAclEntryEntity;
import com.digiaudit.grcone.modules.setup.domain.entity.SystemSetupEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.AppUserEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.BusinessPermissionEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.BusinessPermissionI18nEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.DelegationAssignableRoleEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.DelegationPolicyEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.PermissionEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.PermissionI18nEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.RoleBusinessPermissionEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.RoleEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.RoleI18nEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.RolePermissionEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.UserRoleAssignmentEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

@Configuration(proxyBeanMethods = false)
public class GrconePersistenceConfiguration {

    @Bean
    PersistenceManagedTypes persistenceManagedTypes() {
        return PersistenceManagedTypes.of(
                AuditableEntity.class.getName(),
                com.digiaudit.grcone.modules.datasetprofile.domain.DatasetProfileEntity.class.getName(),
                com.digiaudit.grcone.common.persistence.ProfileFilterDefinition.class.getName(),
                BooleanNumberConverter.class.getName(),
                AuditLogEntity.class.getName(),
                DocumentEntity.class.getName(),
                DocumentLifecycleStatusConverter.class.getName(),
                DocumentLinkEntity.class.getName(),
                DocumentLinkTargetTypeConverter.class.getName(),
                DocumentTempUploadEntity.class.getName(),
                DocumentVersionEntity.class.getName(),
                CentralDefinitionEntity.class.getName(),
LocalOrganizationSubprocessScopeEntity.class.getName(),
                LocalSubprocessControlScopeEntity.class.getName(),
                LocalSubprocessRiskScopeEntity.class.getName(),
                LocalSubprocessControlObjectiveScopeEntity.class.getName(),
                LocalSubprocessRequirementScopeEntity.class.getName(),
                LocalSubprocessRiskControlCoverageEntity.class.getName(),
                LocalSubprocessRiskControlObjectiveCoverageEntity.class.getName(),
                LocalSubprocessControlControlObjectiveCoverageEntity.class.getName(),
                LocalSubprocessRequirementControlCoverageEntity.class.getName(),
                LocalPolicyOrganizationScopeEntity.class.getName(),
                LocalPolicySubprocessScopeEntity.class.getName(),
                LocalPolicyControlScopeEntity.class.getName(),
                LocalPolicyRequirementScopeEntity.class.getName(),
                CentralControlEntity.class.getName(),
                CentralControlGroupEntity.class.getName(),
                CentralControlRelevanceConverter.class.getName(),
                CentralControlObjectiveEntity.class.getName(),
                CentralRiskControlAssignmentEntity.class.getName(),
                CentralAccountGroupRiskAssignmentEntity.class.getName(),
                CentralRiskControlObjectiveAssignmentEntity.class.getName(),
                CentralControlControlObjectiveAssignmentEntity.class.getName(),
                CentralRequirementControlAssignmentEntity.class.getName(),
                CentralControlAccountGroupEntity.class.getName(),
                CentralControlObjectiveAccountGroupEntity.class.getName(),
                CentralSubprocessRiskControlCoverageEntity.class.getName(),
                CentralSubprocessRiskControlObjectiveCoverageEntity.class.getName(),
                CentralSubprocessControlControlObjectiveCoverageEntity.class.getName(),
                CentralSubprocessRequirementControlCoverageEntity.class.getName(),
                CentralRiskCategoryEntity.class.getName(),
                CentralRiskTemplateEntity.class.getName(),
                CentralAccountGroupEntity.class.getName(),
                ControlGroupEntity.class.getName(),
                GlobalControlEntity.class.getName(),
                GlobalControlRegulationEntity.class.getName(),
                ObjectiveEntity.class.getName(),
                OrganizationObjectiveEntity.class.getName(),
                CentralRegulationGroupEntity.class.getName(),
                CentralRegulationEntity.class.getName(),
                CentralRegulationRequirementEntity.class.getName(),
                CentralPolicyGroupEntity.class.getName(),
                CentralPolicyEntity.class.getName(),
                CentralPolicySubprocessScopeEntity.class.getName(),
                CentralPolicyOrganizationScopeEntity.class.getName(),
                CentralPolicyControlScopeEntity.class.getName(),
                CentralPolicyRequirementScopeEntity.class.getName(),
                CentralProcessEntity.class.getName(),
                CentralSubprocessEntity.class.getName(),
                CentralSubprocessControlScopeEntity.class.getName(),
                CentralSubprocessRiskScopeEntity.class.getName(),
                CentralSubprocessControlObjectiveScopeEntity.class.getName(),
                CentralSubprocessRequirementScopeEntity.class.getName(),
                JsonNodeClobConverter.class.getName(),
                MasterDataRevisionContentEntity.class.getName(),
                MasterDataRevisionEntity.class.getName(),
                RevisionEntityTypeConverter.class.getName(),
                MasterDataHierarchyGuardEntity.class.getName(),
                MasterDataLifecycleStatusConverter.class.getName(),
                OrganizationEntity.class.getName(),
                OrganizationTypeConverter.class.getName(),
                ResourceAclEntryEntity.class.getName(),
                SystemSetupEntity.class.getName(),
                AppUserEntity.class.getName(),
                BusinessPermissionEntity.class.getName(),
                BusinessPermissionI18nEntity.class.getName(),
                DelegationAssignableRoleEntity.class.getName(),
                DelegationPolicyEntity.class.getName(),
                PermissionEntity.class.getName(),
                PermissionI18nEntity.class.getName(),
                RoleBusinessPermissionEntity.class.getName(),
                RoleEntity.class.getName(),
                RoleI18nEntity.class.getName(),
                RolePermissionEntity.class.getName(),
                UserRoleAssignmentEntity.class.getName()
        );
    }
}

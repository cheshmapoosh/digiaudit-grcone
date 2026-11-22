package com.digiaudit.grcone.modules.masterdata.coverage.requirementcontrol.api.mapper;

import com.digiaudit.grcone.modules.masterdata.catalog.control.domain.entity.CentralControlEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.regulation.domain.entity.CentralRegulationRequirementEntity;
import com.digiaudit.grcone.modules.masterdata.coverage.requirementcontrol.api.dto.CentralSubprocessRequirementControlCoverageResponse;
import com.digiaudit.grcone.modules.masterdata.coverage.requirementcontrol.domain.entity.CentralSubprocessRequirementControlCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.process.domain.entity.CentralSubprocessEntity;
import com.digiaudit.grcone.modules.masterdata.scope.control.domain.entity.CentralSubprocessControlScopeEntity;
import com.digiaudit.grcone.modules.masterdata.scope.requirement.domain.entity.CentralSubprocessRequirementScopeEntity;
import org.springframework.stereotype.Component;

@Component
public class CentralSubprocessRequirementControlCoverageMapper {
  public CentralSubprocessRequirementControlCoverageResponse toResponse(CentralSubprocessRequirementControlCoverageEntity row, CentralSubprocessEntity subprocess, CentralSubprocessRequirementScopeEntity requirementScope, CentralRegulationRequirementEntity requirement, CentralSubprocessControlScopeEntity controlScope, CentralControlEntity control) {
    return new CentralSubprocessRequirementControlCoverageResponse(row.getId(), row.getSubprocessId(), subprocess.getCode(), subprocess.getTitle(), requirementScope.getId(), requirementScope.getRequirementId(), requirement.getCode(), requirement.getTitle(), requirementScope.getStatus(), requirementScope.getValidFrom(), requirementScope.getValidTo(), controlScope.getId(), controlScope.getControlId(), control.getCode(), control.getTitle(), controlScope.getStatus(), controlScope.getValidFrom(), controlScope.getValidTo(), row.getStatus(), row.getValidFrom(), row.getValidTo(), row.getVersion(), row.getCreatedAt(), row.getCreatedBy(), row.getUpdatedAt(), row.getUpdatedBy(), row.getDeletedAt(), row.getDeletedBy());
  }
}

package com.digiaudit.grcone.modules.masterdata.coverage.controlcontrolobjective.api.mapper;

import com.digiaudit.grcone.modules.masterdata.catalog.control.domain.entity.CentralControlEntity;
import com.digiaudit.grcone.modules.masterdata.catalog.controlobjective.domain.entity.CentralControlObjectiveEntity;
import com.digiaudit.grcone.modules.masterdata.coverage.controlcontrolobjective.api.dto.CentralSubprocessControlControlObjectiveCoverageResponse;
import com.digiaudit.grcone.modules.masterdata.coverage.controlcontrolobjective.domain.entity.CentralSubprocessControlControlObjectiveCoverageEntity;
import com.digiaudit.grcone.modules.masterdata.process.domain.entity.CentralSubprocessEntity;
import com.digiaudit.grcone.modules.masterdata.scope.control.domain.entity.CentralSubprocessControlScopeEntity;
import com.digiaudit.grcone.modules.masterdata.scope.controlobjective.domain.entity.CentralSubprocessControlObjectiveScopeEntity;
import org.springframework.stereotype.Component;

@Component
public class CentralSubprocessControlControlObjectiveCoverageMapper {
  public CentralSubprocessControlControlObjectiveCoverageResponse toResponse(CentralSubprocessControlControlObjectiveCoverageEntity row, CentralSubprocessEntity subprocess, CentralSubprocessControlScopeEntity controlScope, CentralControlEntity control, CentralSubprocessControlObjectiveScopeEntity objectiveScope, CentralControlObjectiveEntity objective) {
    return new CentralSubprocessControlControlObjectiveCoverageResponse(row.getId(), row.getSubprocessId(), subprocess.getCode(), subprocess.getTitle(), controlScope.getId(), controlScope.getControlId(), control.getCode(), control.getTitle(), controlScope.getStatus(), controlScope.getValidFrom(), controlScope.getValidTo(), objectiveScope.getId(), objectiveScope.getControlObjectiveId(), objective.getCode(), objective.getTitle(), objectiveScope.getStatus(), objectiveScope.getValidFrom(), objectiveScope.getValidTo(), row.getStatus(), row.getValidFrom(), row.getValidTo(), row.getVersion(), row.getCreatedAt(), row.getCreatedBy(), row.getUpdatedAt(), row.getUpdatedBy(), row.getDeletedAt(), row.getDeletedBy());
  }
}

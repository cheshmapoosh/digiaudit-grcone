package com.digiaudit.grcone.modules.masterdata.coverage.controlcontrolobjective.api.dto;

import com.digiaudit.grcone.modules.masterdata.scope.control.api.dto.CentralSubprocessControlScopeResponse;
import com.digiaudit.grcone.modules.masterdata.scope.controlobjective.api.dto.CentralSubprocessControlObjectiveScopeResponse;
import java.util.List;

public record CentralControlControlObjectiveCoverageOptionsResponse(
    List<CentralSubprocessControlScopeResponse> controlScopes,
    List<CentralSubprocessControlObjectiveScopeResponse> controlObjectiveScopes) {
  public CentralControlControlObjectiveCoverageOptionsResponse {
    controlScopes = controlScopes == null ? List.of() : List.copyOf(controlScopes);
    controlObjectiveScopes = controlObjectiveScopes == null ? List.of() : List.copyOf(controlObjectiveScopes);
  }
}

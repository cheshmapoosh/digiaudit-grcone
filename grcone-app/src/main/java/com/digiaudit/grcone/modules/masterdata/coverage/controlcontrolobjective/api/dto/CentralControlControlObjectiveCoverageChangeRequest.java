package com.digiaudit.grcone.modules.masterdata.coverage.controlcontrolobjective.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CentralControlControlObjectiveCoverageChangeRequest(
    @NotNull CentralControlControlObjectiveCoverageChangeOperation operation,
    @NotNull UUID controlScopeId,
    @NotNull UUID controlObjectiveScopeId,
    UUID coverageId,
    Long version,
    LocalDate validFrom,
    LocalDate validTo,
    MasterDataLifecycleStatus requestedStatus) {}

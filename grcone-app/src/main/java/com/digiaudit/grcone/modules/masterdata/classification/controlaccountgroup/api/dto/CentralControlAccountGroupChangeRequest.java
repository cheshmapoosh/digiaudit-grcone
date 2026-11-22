package com.digiaudit.grcone.modules.masterdata.classification.controlaccountgroup.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CentralControlAccountGroupChangeRequest(
    @NotNull CentralControlAccountGroupChangeOperation operation,
    @NotNull UUID accountGroupId,
    UUID classificationId,
    Long version,
    LocalDate validFrom,
    LocalDate validTo,
    MasterDataLifecycleStatus requestedStatus) {}

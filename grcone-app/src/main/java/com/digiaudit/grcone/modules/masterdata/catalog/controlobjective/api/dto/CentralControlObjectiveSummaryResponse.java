package com.digiaudit.grcone.modules.masterdata.catalog.controlobjective.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import java.time.LocalDate;
import java.util.UUID;

public record CentralControlObjectiveSummaryResponse(
    UUID id,
    String code,
    String title,
    String objectiveClass,
    MasterDataLifecycleStatus status,
    LocalDate validFrom,
    LocalDate validTo,
    long version) {}

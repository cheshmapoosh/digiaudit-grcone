package com.digiaudit.grcone.modules.masterdata.catalog.control.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import java.time.LocalDate;
import java.util.UUID;

public record CentralControlSummaryResponse(
    UUID id,
    String code,
    String title,
    UUID controlGroupId,
    MasterDataLifecycleStatus status,
    LocalDate validFrom,
    LocalDate validTo,
    long version) {}

package com.digiaudit.grcone.modules.masterdata.scope.control.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CentralSubprocessControlScopeResponse(
    UUID id,
    UUID subprocessId,
    String subprocessCode,
    String subprocessTitle,
    UUID controlId,
    String controlCode,
    String controlTitle,
    String recommendedFrequencyCode,
    String recommendedExecutionMethodCode,
    String recommendedTestMethodCode,
    MasterDataLifecycleStatus status,
    LocalDate validFrom,
    LocalDate validTo,
    long version,
    Instant createdAt,
    UUID createdBy,
    Instant updatedAt,
    UUID updatedBy,
    Instant deletedAt,
    UUID deletedBy) {}

package com.digiaudit.grcone.modules.masterdata.catalog.risk.api.dto;

import com.digiaudit.grcone.modules.masterdata.catalog.risk.domain.enums.CentralRiskType;
import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import java.util.UUID;

public record CentralRiskTemplateSummaryResponse(
    UUID id,
    String code,
    String title,
    UUID riskCategoryId,
    CentralRiskType riskType,
    int sortOrder,
    MasterDataLifecycleStatus status,
    long version) {}

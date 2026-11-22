package com.digiaudit.grcone.modules.masterdata.catalog.risk.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import java.util.List;
import java.util.UUID;

public record CentralRiskCategoryTreeResponse(
    UUID id,
    String code,
    String title,
    UUID parentCategoryId,
    int sortOrder,
    MasterDataLifecycleStatus status,
    long version,
    List<CentralRiskCategoryTreeResponse> children) {
  public CentralRiskCategoryTreeResponse {
    children = children == null ? List.of() : List.copyOf(children);
  }
}

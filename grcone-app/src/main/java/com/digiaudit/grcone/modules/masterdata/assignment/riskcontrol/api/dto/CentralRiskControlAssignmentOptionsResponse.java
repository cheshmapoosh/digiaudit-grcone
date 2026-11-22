package com.digiaudit.grcone.modules.masterdata.assignment.riskcontrol.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CentralRiskControlAssignmentOptionsResponse(List<RiskTemplateOption> riskTemplates) {
  public CentralRiskControlAssignmentOptionsResponse { riskTemplates = List.copyOf(riskTemplates); }
  public record RiskTemplateOption(UUID id, String code, String title,
      MasterDataLifecycleStatus status, LocalDate validFrom, LocalDate validTo) {}
}

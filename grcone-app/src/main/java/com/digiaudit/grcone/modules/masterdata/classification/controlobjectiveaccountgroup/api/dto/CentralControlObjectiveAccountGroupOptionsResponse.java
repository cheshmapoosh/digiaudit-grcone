package com.digiaudit.grcone.modules.masterdata.classification.controlobjectiveaccountgroup.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import java.util.List;
import java.util.UUID;

public record CentralControlObjectiveAccountGroupOptionsResponse(List<AccountGroupOption> accountGroups) {
  public CentralControlObjectiveAccountGroupOptionsResponse { accountGroups = List.copyOf(accountGroups); }

  public record AccountGroupOption(
      UUID id, String code, String title, UUID parentAccountGroupId,
      int sortOrder, MasterDataLifecycleStatus status, List<AccountGroupOption> children) {
    public AccountGroupOption { children = List.copyOf(children); }
  }
}

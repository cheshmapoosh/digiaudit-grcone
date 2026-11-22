package com.digiaudit.grcone.modules.masterdata.accountgroup.domain.value;

public record AccountRangeValue(
        String id,
        String fromAccount,
        String toAccount,
        String description
) {
}

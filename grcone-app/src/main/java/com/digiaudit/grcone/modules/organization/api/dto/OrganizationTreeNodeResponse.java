package com.digiaudit.grcone.modules.organization.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import com.digiaudit.grcone.modules.organization.domain.OrganizationType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrganizationTreeNodeResponse(
        UUID id,
        String code,
        String name,
        OrganizationType organizationType,
        UUID organizationTypeId,
        String organizationTypeName,
        UUID parentOrganizationId,
        String displayLabel,
        MasterDataLifecycleStatus status,
        LocalDate validFrom,
        LocalDate validTo,
        long version,
        List<OrganizationTreeNodeResponse> children
) {
    public OrganizationTreeNodeResponse {
        children = children == null ? List.of() : List.copyOf(children);
    }
}

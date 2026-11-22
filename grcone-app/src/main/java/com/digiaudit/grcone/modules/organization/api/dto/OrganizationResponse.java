package com.digiaudit.grcone.modules.organization.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import com.digiaudit.grcone.modules.organization.domain.OrganizationType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String code,
        String name,
        OrganizationType organizationType,
        UUID organizationTypeId,
        String organizationTypeName,
        UUID parentOrganizationId,
        String displayLabel,
        MasterDataLifecycleStatus status,
        String location,
        String description,
        LocalDate validFrom,
        LocalDate validTo,
        long version,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        Instant deletedAt,
        UUID deletedBy,
        java.util.List<OrganizationOwnerView> ownerAssignments
) {
}

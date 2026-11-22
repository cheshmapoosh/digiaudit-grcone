package com.digiaudit.grcone.modules.organization.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveOrganizationRequest(
        UUID parentOrganizationId,
        @NotNull
        Long version
) {
}

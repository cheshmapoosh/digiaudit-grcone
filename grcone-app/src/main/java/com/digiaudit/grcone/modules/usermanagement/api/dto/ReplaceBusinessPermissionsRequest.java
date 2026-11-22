package com.digiaudit.grcone.modules.usermanagement.api.dto;

import com.digiaudit.grcone.modules.usermanagement.domain.enums.BusinessPermissionCode;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ReplaceBusinessPermissionsRequest(@NotNull Set<BusinessPermissionCode> permissions) {
}

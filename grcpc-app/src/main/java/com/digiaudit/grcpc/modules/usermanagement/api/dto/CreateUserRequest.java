package com.digiaudit.grcpc.modules.usermanagement.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9]+") String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Pattern(regexp = "[0-9]{10}") String nationalCode,
        @Size(max = 20) String mobile,
        @Email @Size(max = 200) String email,
        UUID defaultOrgUnitId,
        Boolean enabled,
        UUID profileId,
        Boolean profileAdmin,
        java.util.List<UUID> profileAccessIds,
        java.util.List<UUID> roleIds,
        @jakarta.validation.Valid java.util.List<ProfileRoleGrantRequest> roleGrants
) {
    private static final java.util.regex.Pattern LOCAL_DIGITS = java.util.regex.Pattern.compile("[۰-۹٠-٩]");

    public CreateUserRequest {
        if (nationalCode != null) {
            nationalCode = LOCAL_DIGITS.matcher(nationalCode.strip())
                    .replaceAll(match -> Integer.toString(Character.digit(match.group().charAt(0), 10)));
        }
    }
}

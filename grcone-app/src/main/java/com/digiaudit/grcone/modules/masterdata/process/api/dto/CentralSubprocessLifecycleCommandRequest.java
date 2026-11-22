package com.digiaudit.grcone.modules.masterdata.process.api.dto;

import jakarta.validation.constraints.NotNull;

public record CentralSubprocessLifecycleCommandRequest(
        @NotNull
        Long version
) {
}

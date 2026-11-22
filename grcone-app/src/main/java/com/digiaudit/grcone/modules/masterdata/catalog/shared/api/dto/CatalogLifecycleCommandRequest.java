package com.digiaudit.grcone.modules.masterdata.catalog.shared.api.dto;

import jakarta.validation.constraints.NotNull;

public record CatalogLifecycleCommandRequest(@NotNull Long version) {}

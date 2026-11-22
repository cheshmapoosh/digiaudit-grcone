package com.digiaudit.grcone.modules.masterdata.scope.control.api.dto;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import java.util.UUID;

/** Read-only projection. A Scope ID identifies a Control occurrence under one Subprocess. */
public record ProcessControlTreeNodeResponse(
    UUID scopeId, UUID subprocessId, UUID controlId, String code, String title,
    MasterDataLifecycleStatus status) {}

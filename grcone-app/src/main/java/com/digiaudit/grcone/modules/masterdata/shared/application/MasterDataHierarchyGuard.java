package com.digiaudit.grcone.modules.masterdata.shared.application;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataHierarchyKey;

public interface MasterDataHierarchyGuard {
    void lock(MasterDataHierarchyKey hierarchyKey);
}

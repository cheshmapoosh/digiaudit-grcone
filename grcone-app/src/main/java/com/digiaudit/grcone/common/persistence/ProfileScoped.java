package com.digiaudit.grcone.common.persistence;

import java.util.UUID;

public interface ProfileScoped {
    UUID getProfileId();
    void setProfileId(UUID profileId);
}

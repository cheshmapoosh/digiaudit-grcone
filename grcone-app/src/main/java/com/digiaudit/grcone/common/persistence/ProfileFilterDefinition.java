package com.digiaudit.grcone.common.persistence;

import com.digiaudit.grcone.common.security.ProfileContext;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@FilterDef(name = "datasetProfile", autoEnabled = true, applyToLoadByKey = true,
        parameters = @ParamDef(name = "profileId", type = byte[].class, resolver = ProfileContext.FilterValue.class))
public abstract class ProfileFilterDefinition {}

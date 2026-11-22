package com.digiaudit.grcone.modules.securityacl.api.mapper;

import com.digiaudit.grcone.modules.securityacl.api.dto.ResourceAclEntryResponse;
import com.digiaudit.grcone.modules.securityacl.domain.entity.ResourceAclEntryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResourceAclEntryMapper {
    ResourceAclEntryResponse toResponse(ResourceAclEntryEntity entity);
}

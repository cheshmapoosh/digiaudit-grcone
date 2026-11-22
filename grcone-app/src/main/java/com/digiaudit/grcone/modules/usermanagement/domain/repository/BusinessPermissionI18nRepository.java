package com.digiaudit.grcone.modules.usermanagement.domain.repository;

import com.digiaudit.grcone.modules.usermanagement.domain.entity.BusinessPermissionI18nEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BusinessPermissionI18nRepository extends JpaRepository<BusinessPermissionI18nEntity, UUID> {
}

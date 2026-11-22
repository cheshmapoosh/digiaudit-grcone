package com.digiaudit.grcone.modules.usermanagement.domain.repository;

import com.digiaudit.grcone.modules.usermanagement.domain.entity.RoleEntity;
import com.digiaudit.grcone.modules.usermanagement.domain.entity.RoleI18nEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleI18nRepository extends JpaRepository<RoleI18nEntity, UUID> {
    void deleteAllByRole(RoleEntity role);
}

package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}

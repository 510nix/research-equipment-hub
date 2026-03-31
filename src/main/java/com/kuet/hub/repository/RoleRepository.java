package com.kuet.hub.repository;

//package com.kuet.researchequipmenthub.repository;

import com.kuet.hub.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(Role.RoleName name);
}
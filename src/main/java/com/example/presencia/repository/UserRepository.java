package com.example.presencia.repository;

import com.example.presencia.model.User;
import com.example.presencia.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByDepartmentId(Long departmentId);

    long countByActiveTrue();
}

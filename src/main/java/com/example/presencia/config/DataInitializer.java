package com.example.presencia.config;

import com.example.presencia.model.Department;
import com.example.presencia.model.User;
import com.example.presencia.model.enums.Role;
import com.example.presencia.repository.DepartmentRepository;
import com.example.presencia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            Department dept = departmentRepository.save(Department.builder()
                    .name("Siege")
                    .description("Bureau principal")
                    .latitude(5.3600)
                    .longitude(-4.0083)
                    .radius(200.0)
                    .build());

            userRepository.save(User.builder()
                    .email("admin@presencia.com")
                    .password(passwordEncoder.encode("admin123"))
                    .prenom("Admin")
                    .nom("Presencia")
                    .matricule("ADM001")
                    .role(Role.ADMIN)
                    .department(dept)
                    .active(true)
                    .build());
        }
    }
}

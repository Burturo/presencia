package com.example.presencia.service;

import com.example.presencia.exception.ResourceNotFoundException;
import com.example.presencia.model.Department;
import com.example.presencia.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departement non trouve: " + id));
    }

    public Department create(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException("Nom de departement deja utilise: " + department.getName());
        }
        return departmentRepository.save(department);
    }

    public Department update(Long id, Department updated) {
        Department dept = findById(id);
        dept.setName(updated.getName());
        dept.setDescription(updated.getDescription());
        dept.setLatitude(updated.getLatitude());
        dept.setLongitude(updated.getLongitude());
        dept.setRadius(updated.getRadius());
        return departmentRepository.save(dept);
    }

    public void delete(Long id) {
        Department dept = findById(id);
        departmentRepository.delete(dept);
    }
}

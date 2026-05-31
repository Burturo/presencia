package com.example.presencia.controller;

import com.example.presencia.model.Department;
import com.example.presencia.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentService.findAll());
        return "departments/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("department", new Department());
        return "departments/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Department department, BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "departments/form";
        }
        departmentService.create(department);
        redirectAttributes.addFlashAttribute("success", "Departement cree avec succes");
        return "redirect:/departments";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("department", departmentService.findById(id));
        return "departments/form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute Department department,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "departments/form";
        }
        departmentService.update(id, department);
        redirectAttributes.addFlashAttribute("success", "Departement modifie avec succes");
        return "redirect:/departments";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        departmentService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Departement supprime avec succes");
        return "redirect:/departments";
    }
}

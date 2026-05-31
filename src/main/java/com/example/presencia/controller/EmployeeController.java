package com.example.presencia.controller;

import com.example.presencia.model.User;
import com.example.presencia.model.enums.Role;
import com.example.presencia.service.DepartmentService;
import com.example.presencia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final UserService userService;
    private final DepartmentService departmentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("employees", userService.findAll());
        return "employees/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("roles", Role.values());
        return "employees/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute User user, BindingResult result,
                          Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("roles", Role.values());
            return "employees/form";
        }
        userService.create(user);
        redirectAttributes.addFlashAttribute("success", "Employe cree avec succes");
        return "redirect:/employees";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("roles", Role.values());
        return "employees/form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute User user,
                          BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("roles", Role.values());
            return "employees/form";
        }
        userService.update(id, user);
        redirectAttributes.addFlashAttribute("success", "Employe modifie avec succes");
        return "redirect:/employees";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Employe supprime avec succes");
        return "redirect:/employees";
    }
}

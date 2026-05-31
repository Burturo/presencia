package com.example.presencia.controller;

import com.example.presencia.service.AttendanceService;
import com.example.presencia.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final DepartmentService departmentService;

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        model.addAttribute("attendances", attendanceService.findByDate(targetDate));
        model.addAttribute("selectedDate", targetDate);
        return "attendance/list";
    }

    @GetMapping("/report")
    public String report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                          Model model) {
        LocalDate s = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate e = end != null ? end : LocalDate.now();
        model.addAttribute("attendances", attendanceService.findByDateRange(s, e));
        model.addAttribute("startDate", s);
        model.addAttribute("endDate", e);
        model.addAttribute("departments", departmentService.findAll());
        return "attendance/report";
    }
}

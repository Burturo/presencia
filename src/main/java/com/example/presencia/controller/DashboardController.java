package com.example.presencia.controller;

import com.example.presencia.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AttendanceService attendanceService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", attendanceService.getDashboardStats());
        model.addAttribute("todayAttendances", attendanceService.findByDate(LocalDate.now()));
        return "dashboard";
    }
}

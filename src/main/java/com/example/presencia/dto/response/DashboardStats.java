package com.example.presencia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DashboardStats {
    private long totalEmployees;
    private long presentToday;
    private long lateToday;
    private long absentToday;
}

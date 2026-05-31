package com.example.presencia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class AttendanceResponse {
    private Long id;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String status;
    private LocalDate date;
    private String notes;
}

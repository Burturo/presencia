package com.example.presencia.dto.response;

import com.example.presencia.model.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
@Builder
@AllArgsConstructor
public class PointageResponse {
    private Long id;
    private String date;
    private String heureEntree;
    private String heureSortie;
    private String statut;
    private Double latitude;
    private Double longitude;
    private String remarque;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static PointageResponse fromAttendance(Attendance a) {
        return PointageResponse.builder()
                .id(a.getId())
                .date(a.getDate().toString())
                .heureEntree(a.getCheckIn() != null ? a.getCheckIn().format(TIME_FMT) : null)
                .heureSortie(a.getCheckOut() != null ? a.getCheckOut().format(TIME_FMT) : null)
                .statut(a.getStatus().name())
                .latitude(a.getCheckInLatitude())
                .longitude(a.getCheckInLongitude())
                .remarque(a.getNotes())
                .build();
    }
}

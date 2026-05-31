package com.example.presencia.api;

import com.example.presencia.dto.request.CheckInRequest;
import com.example.presencia.dto.request.CheckOutRequest;
import com.example.presencia.dto.response.AttendanceResponse;
import com.example.presencia.model.Attendance;
import com.example.presencia.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceApiController {

    private final AttendanceService attendanceService;

    @GetMapping("/today")
    public ResponseEntity<AttendanceResponse> today(@AuthenticationPrincipal UserDetails userDetails) {
        Attendance attendance = attendanceService.getTodayAttendance(userDetails.getUsername());
        if (attendance == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(toResponse(attendance));
    }

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(@AuthenticationPrincipal UserDetails userDetails,
                                                       @Valid @RequestBody CheckInRequest request) {
        Attendance attendance = attendanceService.checkIn(
                userDetails.getUsername(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(toResponse(attendance));
    }

    @PutMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody CheckOutRequest request) {
        Attendance attendance = attendanceService.checkOut(
                userDetails.getUsername(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(toResponse(attendance));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AttendanceResponse>> history(@AuthenticationPrincipal UserDetails userDetails,
                                                             @RequestParam int month,
                                                             @RequestParam int year) {
        List<Attendance> list = attendanceService.getHistory(userDetails.getUsername(), month, year);
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    private AttendanceResponse toResponse(Attendance a) {
        return AttendanceResponse.builder()
                .id(a.getId())
                .checkIn(a.getCheckIn())
                .checkOut(a.getCheckOut())
                .status(a.getStatus().name())
                .date(a.getDate())
                .notes(a.getNotes())
                .build();
    }
}

package com.example.presencia.api;

import com.example.presencia.dto.request.CheckInRequest;
import com.example.presencia.dto.request.CheckOutRequest;
import com.example.presencia.dto.response.PointageResponse;
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
@RequestMapping("/api/pointage")
@RequiredArgsConstructor
public class PointageApiController {

    private final AttendanceService attendanceService;

    @GetMapping("/today")
    public ResponseEntity<PointageResponse> today(@AuthenticationPrincipal UserDetails userDetails) {
        Attendance attendance = attendanceService.getTodayAttendance(userDetails.getUsername());
        if (attendance == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(PointageResponse.fromAttendance(attendance));
    }

    @PostMapping("/check-in")
    public ResponseEntity<PointageResponse> checkIn(@AuthenticationPrincipal UserDetails userDetails,
                                                     @Valid @RequestBody CheckInRequest request) {
        Attendance attendance = attendanceService.checkIn(
                userDetails.getUsername(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(PointageResponse.fromAttendance(attendance));
    }

    @PostMapping("/check-out")
    public ResponseEntity<PointageResponse> checkOut(@AuthenticationPrincipal UserDetails userDetails,
                                                      @Valid @RequestBody CheckOutRequest request) {
        Attendance attendance = attendanceService.checkOut(
                userDetails.getUsername(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(PointageResponse.fromAttendance(attendance));
    }

    @GetMapping("/historique")
    public ResponseEntity<List<PointageResponse>> historique(@AuthenticationPrincipal UserDetails userDetails,
                                                              @RequestParam int mois,
                                                              @RequestParam int annee) {
        List<Attendance> list = attendanceService.getHistory(userDetails.getUsername(), mois, annee);
        return ResponseEntity.ok(list.stream().map(PointageResponse::fromAttendance).toList());
    }
}

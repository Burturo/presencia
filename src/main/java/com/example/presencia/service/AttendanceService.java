package com.example.presencia.service;

import com.example.presencia.dto.response.DashboardStats;
import com.example.presencia.exception.ResourceNotFoundException;
import com.example.presencia.model.Attendance;
import com.example.presencia.model.Department;
import com.example.presencia.model.User;
import com.example.presencia.model.enums.AttendanceStatus;
import com.example.presencia.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserService userService;
    private final GeoLocationService geoLocationService;

    @Value("${app.attendance.start-hour}")
    private String startHour;

    public Attendance checkIn(String email, Double latitude, Double longitude) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now();

        if (attendanceRepository.findByUserIdAndDate(user.getId(), today).isPresent()) {
            throw new IllegalArgumentException("Vous avez deja pointe aujourd'hui");
        }

        Department dept = user.getDepartment();
        if (dept == null) {
            throw new IllegalArgumentException("Aucun departement assigne");
        }

        if (!geoLocationService.isWithinRadius(latitude, longitude,
                dept.getLatitude(), dept.getLongitude(), dept.getRadius())) {
            throw new IllegalArgumentException("Vous n'etes pas dans le perimetre autorise");
        }

        LocalTime limit = LocalTime.parse(startHour);
        AttendanceStatus status = LocalTime.now().isAfter(limit)
                ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        Attendance attendance = Attendance.builder()
                .user(user)
                .checkIn(LocalDateTime.now())
                .checkInLatitude(latitude)
                .checkInLongitude(longitude)
                .status(status)
                .date(today)
                .build();

        return attendanceRepository.save(attendance);
    }

    public Attendance checkOut(String email, Double latitude, Double longitude) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByUserIdAndDate(user.getId(), today)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun pointage trouve pour aujourd'hui"));

        if (attendance.getCheckOut() != null) {
            throw new IllegalArgumentException("Vous avez deja pointe la sortie");
        }

        attendance.setCheckOut(LocalDateTime.now());
        attendance.setCheckOutLatitude(latitude);
        attendance.setCheckOutLongitude(longitude);

        return attendanceRepository.save(attendance);
    }

    public Attendance getTodayAttendance(String email) {
        User user = userService.findByEmail(email);
        return attendanceRepository.findByUserIdAndDate(user.getId(), LocalDate.now())
                .orElse(null);
    }

    public List<Attendance> getHistory(String email, int month, int year) {
        User user = userService.findByEmail(email);
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return attendanceRepository.findByUserIdAndDateBetweenOrderByDateDesc(user.getId(), start, end);
    }

    public List<Attendance> findByDate(LocalDate date) {
        return attendanceRepository.findByDateOrderByCheckInDesc(date);
    }

    public List<Attendance> findByDateRange(LocalDate start, LocalDate end) {
        return attendanceRepository.findByDateBetweenOrderByDateDesc(start, end);
    }

    public DashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();
        return DashboardStats.builder()
                .totalEmployees(userService.countActiveEmployees())
                .presentToday(attendanceRepository.countByDateAndStatus(today, AttendanceStatus.PRESENT))
                .lateToday(attendanceRepository.countByDateAndStatus(today, AttendanceStatus.LATE))
                .absentToday(userService.countActiveEmployees()
                        - attendanceRepository.countByDate(today))
                .build();
    }
}

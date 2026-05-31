package com.example.presencia.repository;

import com.example.presencia.model.Attendance;
import com.example.presencia.model.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserIdAndDate(Long userId, LocalDate date);

    List<Attendance> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end);

    List<Attendance> findByDateOrderByCheckInDesc(LocalDate date);

    List<Attendance> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);

    @Query("SELECT a FROM Attendance a WHERE a.user.department.id = :deptId AND a.date = :date")
    List<Attendance> findByDepartmentAndDate(@Param("deptId") Long departmentId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.date = :date")
    long countByDate(@Param("date") LocalDate date);
}

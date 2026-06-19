package com.example.presencia.service;

import com.example.presencia.model.Attendance;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final AttendanceService attendanceService;

    public byte[] exportAttendanceToExcel(LocalDate start, LocalDate end) throws IOException {
        List<Attendance> attendances = attendanceService.findByDateRange(start, end);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Pointages");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            Row header = sheet.createRow(0);
            String[] columns = {"Date", "Employe", "Email", "Departement", "Entree", "Sortie", "Statut"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Attendance a : attendances) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                row.createCell(1).setCellValue(a.getUser().getPrenom() + " " + a.getUser().getNom());
                row.createCell(2).setCellValue(a.getUser().getEmail());
                row.createCell(3).setCellValue(
                        a.getUser().getDepartment() != null ? a.getUser().getDepartment().getName() : "");
                row.createCell(4).setCellValue(a.getCheckIn().format(dtf));
                row.createCell(5).setCellValue(a.getCheckOut() != null ? a.getCheckOut().format(dtf) : "");
                row.createCell(6).setCellValue(a.getStatus().name());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}

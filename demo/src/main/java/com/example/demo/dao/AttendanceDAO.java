package com.example.demo.dao;

import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

@Repository
public class AttendanceDAO {
    private final DataSource dataSource;

    public AttendanceDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Подсчет пропусков по дисциплине (для автозапрета)
    public long countAbsences(Integer studentId, Integer disciplineId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND discipline_id = ? AND attend = false";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, disciplineId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    // Добавление записи о посещаемости
    public void markAttendance(Integer studentId, Integer disciplineId, boolean isPresent, String classDate) throws SQLException {
        String sql = "INSERT INTO attendance (student_id, discipline_id, attend, class_type, class_date) VALUES (?, ?, ?, 'ЛК', ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, disciplineId);
            pstmt.setBoolean(3, isPresent);
            pstmt.setDate(4, Date.valueOf(classDate));
            pstmt.executeUpdate();
        }
    }
}
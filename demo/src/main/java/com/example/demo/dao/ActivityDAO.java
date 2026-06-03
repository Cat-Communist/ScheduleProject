package com.example.demo.dao;

import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Repository
public class ActivityDAO {
    private final DataSource dataSource;

    public ActivityDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Регистрация на мероприятие
    public void registerStudent(Integer studentId, Integer activityId) throws SQLException {
        String sql = "INSERT INTO gr_act_stats (student_id, act_id, status) VALUES (?, ?, true)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, activityId);
            pstmt.executeUpdate();
        }
    }

    // Формирование отчета (Native JDBC mapping to Map)
    public List<Map<String, Object>> getEventReport(Integer activityId) throws SQLException {
        String sql = "SELECT s.fullname, COALESCE(gas.status, false) as attended " +
                "FROM students s " +
                "JOIN group_and_activities gaa ON s.group_id = gaa.group_id " +
                "LEFT JOIN gr_act_stats gas ON s.id = gas.student_id AND gaa.activity_id = gas.act_id " +
                "WHERE gaa.activity_id = ?";

        List<Map<String, Object>> report = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, activityId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("fullname", rs.getString("fullname"));
                    row.put("attended", rs.getBoolean("attended"));
                    report.add(row);
                }
            }
        }
        return report;
    }

    // Проверка даты мероприятия для документа
    public Date getActivityDate(Integer activityId) throws SQLException {
        String sql = "SELECT start_date FROM group_activities WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, activityId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDate("start_date");
            }
        }
        return null;
    }
}
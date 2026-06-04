package com.example.demo.service;

import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class EventService {
    private final DataSource dataSource;

    public EventService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 1. ПОЛУЧИТЬ ВСЕ МЕРОПРИЯТИЯ
    public List<Map<String, Object>> getAllEvents() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        String sql = "SELECT id, title, start_date, finish_date FROM group_activities ORDER BY start_date DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> event = new HashMap<>();
                event.put("id", rs.getInt("id"));
                event.put("title", rs.getString("title"));
                event.put("date", rs.getString("start_date"));
                event.put("description", "До: " + rs.getString("finish_date"));
                events.add(event);
            }
        }
        return events;
    }

    // 2. ЗАПИСЬ НА МЕРОПРИЯТИЕ (проверка по таблице attendance)
    public String registerForEvent(Integer studentId, Integer activityId, Integer unitId) throws Exception {
        System.out.println("=== ПРОВЕРКА ПРОПУСКОВ ===");
        System.out.println("studentId: " + studentId);
        System.out.println("unitId (discipline_id): " + unitId);

        String checkSql = "SELECT COUNT(*) as skip_count FROM attendance " +
                "WHERE student_id = ? AND discipline_id = ? AND attend = false";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, unitId);

            System.out.println("SQL: " + checkSql);
            System.out.println("Параметры: studentId=" + studentId + ", discipline_id=" + unitId);

            ResultSet rs = ps.executeQuery();
            rs.next();
            int skipCount = rs.getInt("skip_count");

            System.out.println("Найдено пропусков: " + skipCount);

            if (skipCount >= 3) {
                throw new Exception("АВТОЗАПРЕТ: У студента " + skipCount +
                        " пропусков по дисциплине (норма: не более 2). Запись запрещена.");
            }
        }

        return "Студент допущен к участию (пропусков по дисциплине: " +
                getSkipCountFromAttendance(studentId, unitId) + " из 3 допустимых).";
    }

    // Считает пропуски по дисциплине из таблицы attendance
    private int getSkipCountFromAttendance(Integer studentId, Integer unitId) throws Exception {
        String sql = "SELECT COUNT(*) FROM attendance " +
                "WHERE student_id = ? AND discipline_id = ? AND attend = false";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, unitId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    // Вспомогательный метод для подсчёта пропусков
    private int getSkipCount(Integer studentId) throws Exception {
        String sql = "SELECT COUNT(*) FROM gr_act_stats WHERE student_id = ? AND status = false";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    // 3. СТУДЕНТЫ НА МЕРОПРИЯТИИ (Через группы!)
    public List<Map<String, Object>> getEventStudents(Integer activityId) throws Exception {
        List<Map<String, Object>> students = new ArrayList<>();
        // Цепочка: Мероприятие -> Группы на мероприятии -> Студенты в этих группах
        String sql = "SELECT DISTINCT s.id, s.fullname " +
                "FROM students s " +
                "JOIN groups g ON s.group_id = g.id " +
                "JOIN group_and_activities gaa ON g.id = gaa.group_id " +
                "WHERE gaa.activity_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> student = new HashMap<>();
                student.put("id", rs.getInt("id"));
                student.put("fullname", rs.getString("fullname"));
                students.add(student);
            }
        }
        return students;
    }

    // 4. ОТМЕТКА ПОСЕЩАЕМОСТИ
    public void markAttendance(Integer starostaId, Integer studentId,
                               Integer activityId, String date, boolean isPresent) throws Exception {

        // activityId теперь передаётся напрямую, поиск не нужен!
        if (activityId == null) {
            throw new Exception("Не указан ID мероприятия");
        }

        // Проверяем, что мероприятие существует
        String checkSql = "SELECT id FROM group_activities WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, activityId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new Exception("Мероприятие с ID " + activityId + " не найдено");
            }
        }

        // Вставляем или обновляем отметку
        String sql = "INSERT INTO gr_act_stats (student_id, act_id, status) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT (student_id, act_id) DO UPDATE SET status = EXCLUDED.status";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, activityId);
            ps.setBoolean(3, isPresent);
            ps.executeUpdate();
        }
    }

    // 5. ФОРМИРОВАНИЕ РАЗРЕШЕНИЯ
    public String generatePermissionDocument(Integer activityId) throws Exception {
        String eventSql = "SELECT title, start_date FROM group_activities WHERE id = ?";
        String title = null;
        String eventDateStr = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(eventSql)) {
            ps.setInt(1, activityId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                title = rs.getString("title");
                eventDateStr = rs.getString("start_date");
            }
        }

        if (title == null) throw new Exception("Мероприятие не найдено");

        // Список студентов через группы
        String studentsSql = "SELECT DISTINCT s.fullname " +
                "FROM students s " +
                "JOIN groups g ON s.group_id = g.id " +
                "JOIN group_and_activities gaa ON g.id = gaa.group_id " +
                "WHERE gaa.activity_id = ?";

        List<String> students = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(studentsSql)) {
            ps.setInt(1, activityId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) students.add(rs.getString("fullname"));
        }

        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        StringBuilder doc = new StringBuilder();
        doc.append("РАЗРЕШЕНИЕ НА ПОСЕЩЕНИЕ МЕРОПРИЯТИЯ\n");
        doc.append("=====================================\n\n");
        doc.append("Мероприятие: ").append(title).append("\n");
        doc.append("Дата начала: ").append(eventDateStr).append("\n\n");
        doc.append("СПИСОК ДОПУЩЕННЫХ (по группам):\n");
        doc.append("-----------------\n");
        for (int i = 0; i < students.size(); i++) {
            doc.append((i + 1)).append(". ").append(students.get(i)).append("\n");
        }
        doc.append("\nВсего допущено: ").append(students.size()).append(" чел.\n");
        doc.append("Дата формирования: ").append(today).append("\n");

        return doc.toString();
    }

    // 6. ОТЧЁТ
    public List<Map<String, Object>> getReport(Integer activityId) throws Exception {
        List<Map<String, Object>> report = new ArrayList<>();
        // Отчет по студентам, которые должны быть на мероприятии (через группы)
        String sql = "SELECT DISTINCT s.fullname, gas.status " +
                "FROM students s " +
                "JOIN groups g ON s.group_id = g.id " +
                "JOIN group_and_activities gaa ON g.id = gaa.group_id " +
                "LEFT JOIN gr_act_stats gas ON s.id = gas.student_id AND gaa.activity_id = gas.act_id " +
                "WHERE gaa.activity_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("fullname", rs.getString("fullname"));
                // Обратите внимание: поле теперь называется status
                row.put("attended", rs.getObject("status") != null ? rs.getBoolean("status") : null);
                report.add(row);
            }
        }
        return report;
    }
}
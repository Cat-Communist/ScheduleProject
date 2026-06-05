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
        String sql = "SELECT id, title, start_date, finish_date, unit_id FROM group_activities ORDER BY start_date DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> event = new HashMap<>();
                event.put("id", rs.getInt("id"));
                event.put("title", rs.getString("title"));
                event.put("date", rs.getString("start_date"));
                event.put("description", "До: " + rs.getString("finish_date"));
                event.put("unit_id", rs.getInt("unit_id"));
                events.add(event);
            }
        }
        return events;
    }

    // 2. ЗАПИСЬ НА МЕРОПРИЯТИЕ (проверка пропусков через таблицу-связку)
    public String registerForEvent(Integer studentId, Integer activityId, Integer unitId) throws Exception {
        System.out.println("=== ПРОВЕРКА ПРОПУСКОВ ===");
        System.out.println("studentId: " + studentId);
        System.out.println("activityId: " + activityId);
        System.out.println("unitId: " + unitId);

        // Получаем все discipline_id для этого unit_id
        String getDisciplinesSql = "SELECT discipline_id FROM unit_disciplines WHERE unit_id = ?";
        List<Integer> disciplineIds = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(getDisciplinesSql)) {
            ps.setInt(1, unitId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                disciplineIds.add(rs.getInt("discipline_id"));
            }
        }

        System.out.println("Дисциплины подразделения " + unitId + ": " + disciplineIds);

        if (disciplineIds.isEmpty()) {
            return "Студент допущен (нет связанных дисциплин).";
        }

        int skipCount = 0;

        // Считаем пропуски по ВСЕМ дисциплинам этого подразделения
        StringBuilder checkSql = new StringBuilder(
                "SELECT COUNT(*) as skip_count FROM attendance " +
                        "WHERE student_id = ? AND discipline_id IN ("
        );

        for (int i = 0; i < disciplineIds.size(); i++) {
            if (i > 0) checkSql.append(", ");
            checkSql.append("?");
        }
        checkSql.append(") AND attend = false");

        System.out.println("SQL: " + checkSql.toString());

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql.toString())) {
            ps.setInt(1, studentId);
            for (int i = 0; i < disciplineIds.size(); i++) {
                ps.setInt(i + 2, disciplineIds.get(i));
            }

            ResultSet rs = ps.executeQuery();
            rs.next();
            skipCount = rs.getInt("skip_count"); // Присваиваем внешней переменной

            System.out.println("Найдено пропусков: " + skipCount);

            if (skipCount >= 3) {
                throw new Exception("АВТОЗАПРЕТ: У студента " + skipCount +
                        " пропусков по дисциплинам этого подразделения (норма: не более 2).");
            }
        }

        return "Студент допущен (пропусков: " + skipCount + " из 3).";
    }

    // 3. СТУДЕНТЫ НА МЕРОПРИЯТИИ (Через группы!)
    public List<Map<String, Object>> getEventStudents(Integer activityId) throws Exception {
        List<Map<String, Object>> students = new ArrayList<>();
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

        if (activityId == null) {
            throw new Exception("Не указан ID мероприятия");
        }

        String checkSql = "SELECT id FROM group_activities WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, activityId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new Exception("Мероприятие с ID " + activityId + " не найдено");
            }
        }

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

    // 5. ФОРМИРОВАНИЕ РАЗРЕШЕНИЯ (с проверкой через таблицу-связку)
    public String generatePermissionDocument(Integer activityId) throws Exception {
        String eventSql = "SELECT title, start_date, unit_id FROM group_activities WHERE id = ?";
        String title = null;
        String eventDateStr = null;
        Integer unitId = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(eventSql)) {
            ps.setInt(1, activityId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                title = rs.getString("title");
                eventDateStr = rs.getString("start_date");
                unitId = rs.getInt("unit_id");
            }
        }

        if (title == null) throw new Exception("Мероприятие не найдено");

        String getDisciplinesSql = "SELECT discipline_id FROM unit_disciplines WHERE unit_id = ?";
        List<Integer> disciplineIds = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(getDisciplinesSql)) {
            ps.setInt(1, unitId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                disciplineIds.add(rs.getInt("discipline_id"));
            }
        }

        System.out.println("=== ФОРМИРОВАНИЕ РАЗРЕШЕНИЯ ===");
        System.out.println("unitId: " + unitId);
        System.out.println("disciplineIds: " + disciplineIds);

        String studentsSql = "SELECT DISTINCT s.id, s.fullname " +
                "FROM students s " +
                "JOIN groups g ON s.group_id = g.id " +
                "JOIN group_and_activities gaa ON g.id = gaa.group_id " +
                "WHERE gaa.activity_id = ?";

        List<String> allowedStudents = new ArrayList<>();
        List<String> blockedStudents = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(studentsSql)) {
            ps.setInt(1, activityId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int studentId = rs.getInt("id");
                String fullname = rs.getString("fullname");

                int skipCount = getSkipCountByDisciplines(studentId, disciplineIds);

                System.out.println("Студент: " + fullname + ", пропусков: " + skipCount);

                if (skipCount < 3) {
                    allowedStudents.add(fullname);
                } else {
                    blockedStudents.add(fullname + " (" + skipCount + " пропусков)");
                }
            }
        }

        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        StringBuilder doc = new StringBuilder();
        doc.append("РАЗРЕШЕНИЕ НА ПОСЕЩЕНИЕ МЕРОПРИЯТИЯ\n");
        doc.append("=====================================\n\n");
        doc.append("Мероприятие: ").append(title).append("\n");
        doc.append("Дата начала: ").append(eventDateStr).append("\n");
        doc.append("Подразделение: ").append(getUnitName(unitId)).append("\n\n");
        doc.append("СПИСОК ДОПУЩЕННЫХ:\n");
        doc.append("-----------------\n");

        if (allowedStudents.isEmpty()) {
            doc.append("Нет допущенных студентов (у всех >= 3 пропусков)\n");
        } else {
            for (int i = 0; i < allowedStudents.size(); i++) {
                doc.append((i + 1)).append(". ").append(allowedStudents.get(i)).append("\n");
            }
        }

        doc.append("\nВсего допущено: ").append(allowedStudents.size()).append(" чел.\n");

        if (!blockedStudents.isEmpty()) {
            doc.append("\nНЕ ДОПУЩЕНЫ (>= 3 пропусков):\n");
            doc.append("-----------------\n");
            for (String blocked : blockedStudents) {
                doc.append("• ").append(blocked).append("\n");
            }
        }

        doc.append("\nДата формирования: ").append(today).append("\n");

        return doc.toString();
    }

    // Считает пропуски по списку дисциплин
    private int getSkipCountByDisciplines(Integer studentId, List<Integer> disciplineIds) throws Exception {
        if (disciplineIds.isEmpty()) {
            return 0;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM attendance " +
                        "WHERE student_id = ? AND discipline_id IN ("
        );

        for (int i = 0; i < disciplineIds.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(") AND attend = false");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, studentId);
            for (int i = 0; i < disciplineIds.size(); i++) {
                ps.setInt(i + 2, disciplineIds.get(i));
            }
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    // Вспомогательный метод для получения названия подразделения
    private String getUnitName(Integer unitId) throws Exception {
        String sql = "SELECT title FROM unit WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, unitId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("title");
            return "неизвестно";
        }
    }

    // 6. ОТЧЁТ
    public List<Map<String, Object>> getReport(Integer activityId) throws Exception {
        List<Map<String, Object>> report = new ArrayList<>();
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
                row.put("attended", rs.getObject("status") != null ? rs.getBoolean("status") : null);
                report.add(row);
            }
        }
        return report;
    }
}
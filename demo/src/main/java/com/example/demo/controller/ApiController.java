package com.example.demo.controller;

import com.example.demo.service.EventService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {
    private final EventService eventService;
    private final DataSource dataSource; // ✅ ДОБАВЛЕНО

    // ✅ ИЗМЕНЁН КОНСТРУКТОР - теперь принимает и EventService, и DataSource
    public ApiController(EventService eventService, DataSource dataSource) {
        this.eventService = eventService;
        this.dataSource = dataSource;
    }

    // ✅ НОВЫЙ МЕТОД: Логин
    @PostMapping("login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> credentials) {
        try {
            String login = credentials.get("login");
            String password = credentials.get("password");

            String sql = "SELECT id, fullname, role FROM students WHERE login = ? AND password = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, login);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int id = rs.getInt("id");
                    String fullname = rs.getString("fullname");
                    String role = rs.getString("role");

                    // Возвращаем данные в формате "id|fullname|role"
                    return ResponseEntity.ok(id + "|" + fullname + "|" + role);
                } else {
                    return ResponseEntity.status(401).body("Неверный логин или пароль");
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка сервера: " + e.getMessage());
        }
    }

    // 1. ЛЕНТА МЕРОПРИЯТИЙ
    @GetMapping("events")
    public List<Map<String, Object>> getEvents() throws Exception {
        return eventService.getAllEvents();
    }

    // 2. ЗАПИСЬ НА МЕРОПРИЯТИЕ
    @PostMapping("register")
    public ResponseEntity<String> register(
            @RequestParam Integer studentId,
            @RequestParam Integer activityId,
            @RequestParam Integer unitId) {

        // ✅ ДОБАВЬТЕ ЛОГИРОВАНИЕ
        System.out.println("=== ЗАПРОС НА ЗАПИСЬ ===");
        System.out.println("studentId: " + studentId);
        System.out.println("activityId: " + activityId);
        System.out.println("unitId: " + unitId);

        try {
            String result = eventService.registerForEvent(studentId, activityId, unitId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.out.println("ОШИБКА: " + e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // 3. СПИСОК СТУДЕНТОВ НА МЕРОПРИЯТИИ
    @GetMapping("event/{activityId}/students")
    public List<Map<String, Object>> getEventStudents(@PathVariable Integer activityId) throws Exception {
        return eventService.getEventStudents(activityId);
    }

    // МАССОВАЯ ОТМЕТКА ПОСЕЩАЕМОСТИ
    @PostMapping("attendance/batch")
    public ResponseEntity<String> markAttendanceBatch(@RequestBody List<Map<String, Object>> items) {
        try {
            for (Map<String, Object> item : items) {
                int studentId = ((Number) item.get("studentId")).intValue();
                int activityId = ((Number) item.get("activityId")).intValue();
                String date = (String) item.get("date");
                boolean isPresent = (boolean) item.get("isPresent");

                // Передаём activityId напрямую, а не unitId
                eventService.markAttendance(1, studentId, activityId, date, isPresent);
            }
            return ResponseEntity.ok("Сохранено для " + items.size() + " студентов");
        } catch (Exception e) {
            System.out.println("Ошибка при отметке: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(400).body("Ошибка: " + e.getMessage());
        }
    }

    // 5. ФОРМИРОВАНИЕ РАЗРЕШЕНИЯ
    @GetMapping("document/{activityId}")
    public ResponseEntity<String> getDocument(@PathVariable Integer activityId) {
        try {
            String doc = eventService.generatePermissionDocument(activityId);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // 6. ОТЧЁТ ПО МЕРОПРИЯТИЮ
    @GetMapping("report/{activityId}")
    public List<Map<String, Object>> getReport(@PathVariable Integer activityId) throws Exception {
        return eventService.getReport(activityId);
    }
}
package com.example.demo.service;

import com.example.demo.dao.*;
import com.example.demo.model.Student;
import org.springframework.stereotype.Service;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class EventService {
    private final StudentDAO studentDAO;
    private final AttendanceDAO attendanceDAO;
    private final ActivityDAO activityDAO;

    public EventService(StudentDAO studentDAO, AttendanceDAO attendanceDAO, ActivityDAO activityDAO) {
        this.studentDAO = studentDAO;
        this.attendanceDAO = attendanceDAO;
        this.activityDAO = activityDAO;
    }

    public Student login(String login, String password) throws Exception {
        Student student = studentDAO.authenticate(login, password);
        if (student == null) throw new Exception("Неверный логин или пароль");
        return student;
    }

    public String registerForEvent(Integer studentId, Integer activityId, Integer disciplineId) throws Exception {
        long absences = attendanceDAO.countAbsences(studentId, disciplineId);
        if (absences >= 3) {
            throw new Exception("АВТОЗАПРЕТ: У студента 3 и более пропусков по данной дисциплине.");
        }
        activityDAO.registerStudent(studentId, activityId);
        return "Успешная регистрация на мероприятие";
    }

    public String markAttendance(Integer starostaId, Integer studentId, Integer disciplineId, String classDate, boolean isPresent) throws Exception {
        Student starosta = studentDAO.authenticate("starosta", "123"); // Упрощенная проверка для демо
        // В реальном приложении здесь была бы проверка starosta.getId().equals(starostaId)

        attendanceDAO.markAttendance(studentId, disciplineId, isPresent, classDate);
        return "Посещаемость успешно отмечена";
    }

    public String generatePermissionDocument(Integer activityId) throws Exception {
        Date activityDate = activityDAO.getActivityDate(activityId);
        if (activityDate == null) throw new Exception("Мероприятие не найдено");

        Date tomorrow = Date.valueOf(LocalDate.now().plusDays(1));
        if (!activityDate.equals(tomorrow)) {
            throw new Exception("Документ формируется только за 1 день до мероприятия!");
        }

        return "=== РАЗРЕШЕНИЕ НА ПОСЕЩЕНИЕ ===\n" +
                "Мероприятие ID: " + activityId + "\n" +
                "Дата проведения: " + activityDate + "\n" +
                "Статус: СОГЛАСОВАНО Администрацией.\n" +
                "===============================";
    }

    public List<Map<String, Object>> getReport(Integer activityId) throws Exception {
        return activityDAO.getEventReport(activityId);
    }
}
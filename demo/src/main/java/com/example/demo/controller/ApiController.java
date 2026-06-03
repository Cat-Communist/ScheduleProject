package com.example.demo.controller;

import com.example.demo.model.Student;
import com.example.demo.service.EventService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {
    private final EventService eventService;

    public ApiController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/login")
    public Student login(@RequestBody Map<String, String> credentials) throws Exception {
        return eventService.login(credentials.get("login"), credentials.get("password"));
    }

    @PostMapping("/register")
    public String register(@RequestParam Integer studentId, @RequestParam Integer activityId, @RequestParam Integer disciplineId) throws Exception {
        return eventService.registerForEvent(studentId, activityId, disciplineId);
    }

    @PostMapping("/attendance")
    public String markAttendance(@RequestParam Integer starostaId, @RequestParam Integer studentId,
                                 @RequestParam Integer disciplineId, @RequestParam String date, @RequestParam boolean isPresent) throws Exception {
        return eventService.markAttendance(starostaId, studentId, disciplineId, date, isPresent);
    }

    @GetMapping("/document/{activityId}")
    public String getDocument(@PathVariable Integer activityId) throws Exception {
        return eventService.generatePermissionDocument(activityId);
    }

    @GetMapping("/report/{activityId}")
    public List<Map<String, Object>> getReport(@PathVariable Integer activityId) throws Exception {
        return eventService.getReport(activityId);
    }
}
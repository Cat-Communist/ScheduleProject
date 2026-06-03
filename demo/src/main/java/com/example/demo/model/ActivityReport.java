package com.example.demo.model;

public class ActivityReport {
    private int studentId;
    private String fullname;
    private boolean attended; // Статус участия в мероприятии

    public ActivityReport() {
    }

    // Геттеры и Сеттеры
    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public boolean isAttended() {
        return attended;
    }

    public void setAttended(boolean attended) {
        this.attended = attended;
    }
}
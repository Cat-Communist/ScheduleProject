package com.example.scheduleapp.network;

public class AttendanceItem {
    public int studentId;
    public int activityId;
    public int disciplineId;
    public String date;
    public boolean isPresent;

    public AttendanceItem(int studentId, int activityId, int disciplineId,
                          String date, boolean isPresent) {
        this.studentId = studentId;
        this.activityId = activityId;
        this.disciplineId = disciplineId;
        this.date = date;
        this.isPresent = isPresent;
    }
}
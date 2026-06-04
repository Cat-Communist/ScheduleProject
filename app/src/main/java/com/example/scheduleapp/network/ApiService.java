package com.example.scheduleapp.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;

public interface ApiService {

    @POST("login")
    Call<ResponseBody> login(@Body Map<String, String> credentials);

    @GET("events")
    Call<List<Map<String, Object>>> getEvents();

    // ✅ ЯВНО УКАЗЫВАЕМ POST
    @POST("register")
    Call<ResponseBody> register(@Query("studentId") Integer studentId,
                                @Query("activityId") Integer activityId,
                                @Query("unitId") Integer unitId);

    @GET("event/{activityId}/students")
    Call<List<Map<String, Object>>> getEventStudents(@Path("activityId") Integer activityId);

    @POST("attendance/batch")
    Call<ResponseBody> markAttendanceBatch(@Body List<AttendanceItem> items);

    @GET("document/{activityId}")
    Call<ResponseBody> getDocument(@Path("activityId") Integer activityId);

    @GET("report/{activityId}")
    Call<List<Map<String, Object>>> getReport(@Path("activityId") Integer activityId);
}
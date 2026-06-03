package com.example.demo.network;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Авторизация
    @POST("login")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    // Регистрация на мероприятие (с проверкой автозапрета на сервере)
    @POST("register")
    Call<String> registerForEvent(
            @Query("studentId") Integer studentId,
            @Query("activityId") Integer activityId,
            @Query("disciplineId") Integer disciplineId
    );

    // Отметка посещаемости (только для старосты)
    @POST("attendance")
    Call<String> markAttendance(
            @Query("starostaId") Integer starostaId,
            @Query("studentId") Integer studentId,
            @Query("disciplineId") Integer disciplineId,
            @Query("date") String date,
            @Query("isPresent") boolean isPresent
    );

    // Получение документа-разрешения
    @GET("document/{activityId}")
    Call<String> getPermissionDocument(@Path("activityId") Integer activityId);

    // Получение отчета по мероприятию
    @GET("report/{activityId}")
    Call<List<Map<String, Object>>> getEventReport(@Path("activityId") Integer activityId);
}
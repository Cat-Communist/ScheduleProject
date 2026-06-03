package com.example.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.demo.network.RetrofitClient;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvResult;
    private Spinner spinnerRole;
    private int currentUserId = 1; // По умолчанию Староста (ID из БД)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tvResult);
        spinnerRole = findViewById(R.id.spinnerRole);

        // Настройка выпадающего списка для смены ролей
        String[] roles = {"Староста (ID: 1)", "Студент Хорошист (ID: 2)", "Студент Прогульщик (ID: 3)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // ID пользователей соответствуют тем, что мы добавили в SQL скрипте
                if (position == 0) currentUserId = 1;
                else if (position == 1) currentUserId = 2;
                else currentUserId = 3;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 1. Кнопка получения отчета
        findViewById(R.id.btnReport).setOnClickListener(v -> {
            RetrofitClient.getService().getEventReport(1).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        StringBuilder sb = new StringBuilder("ОТЧЕТ ПО МЕРОПРИЯТИЮ #1:\n\n");
                        for (Map<String, Object> row : response.body()) {
                            sb.append("Студент: ").append(row.get("fullname"))
                                    .append("\nПрисутствовал: ").append(row.get("attended")).append("\n---\n");
                        }
                        tvResult.setText(sb.toString());
                    } else {
                        tvResult.setText("Ошибка получения отчета");
                    }
                }
                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    tvResult.setText("Ошибка сети: " + t.getMessage());
                }
            });
        });

        // 2. Кнопка отметки посещаемости (только для старосты)
        findViewById(R.id.btnMark).setOnClickListener(v -> {
            if (currentUserId != 1) {
                tvResult.setText("ОШИБКА: Только староста (ID 1) может отмечать посещаемость!");
                return;
            }
            // Отмечаем студента ID 3 как отсутствующего по дисциплине ID 1
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            RetrofitClient.getService().markAttendance(1, 3, 1, today, false).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        tvResult.setText("УСПЕХ: " + response.body());
                    } else {
                        tvResult.setText("ОШИБКА: " + response.errorBody().toString());
                    }
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    tvResult.setText("Ошибка сети: " + t.getMessage());
                }
            });
        });

        // 3. Кнопка регистрации (демонстрация автозапрета)
        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            // Пытаемся записать текущего пользователя на мероприятие ID 1 по дисциплине ID 1
            RetrofitClient.getService().registerForEvent(currentUserId, 1, 1).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        tvResult.setText("УСПЕХ: " + response.body());
                    } else {
                        // Здесь мы поймаем ошибку "Автозапрет" от сервера
                        tvResult.setText("ОТКАЗ СИСТЕМЫ:\n" + response.errorBody().toString());
                    }
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    tvResult.setText("Ошибка сети: " + t.getMessage());
                }
            });
        });

        // 4. Кнопка формирования документа
        findViewById(R.id.btnDoc).setOnClickListener(v -> {
            RetrofitClient.getService().getPermissionDocument(1).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        tvResult.setText("ДОКУМЕНТ СФОРМИРОВАН:\n\n" + response.body());
                    } else {
                        tvResult.setText("ОШИБКА: " + response.errorBody().toString());
                    }
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    tvResult.setText("Ошибка сети: " + t.getMessage());
                }
            });
        });
    }
}
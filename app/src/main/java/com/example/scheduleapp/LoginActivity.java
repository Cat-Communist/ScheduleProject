package com.example.scheduleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scheduleapp.network.RetrofitClient;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private TextView tvError;

    public static int CURRENT_USER_ID = -1;
    public static String CURRENT_USER_ROLE = "";
    public static String CURRENT_USER_NAME = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        Button btnLogin = findViewById(R.id.btnLogin);

        // Предзаполнение для быстрого тестирования
        etLogin.setText("starosta");
        etPassword.setText("123");

        btnLogin.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (login.isEmpty() || password.isEmpty()) {
                tvError.setText("Введите логин и пароль");
                return;
            }

            loginUser(login, password);
        });
    }

    private void loginUser(String login, String password) {
        tvError.setText("Проверка...");

        // Запрос к серверу для проверки логина/пароля
        Map<String, String> credentials = new java.util.HashMap<>();
        credentials.put("login", login);
        credentials.put("password", password);

        RetrofitClient.getService().login(credentials).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        // Успешный вход - сервер вернул данные пользователя
                        String userData = response.body().string();
                        parseAndLogin(userData);
                    } else {
                        String error = response.errorBody() != null ?
                                response.errorBody().string() : "Неверный логин или пароль";
                        tvError.setText(error);
                    }
                } catch (Exception e) {
                    tvError.setText("Ошибка: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                tvError.setText("Сетевая ошибка: " + t.getMessage());
            }
        });
    }

    private void parseAndLogin(String userData) {
        // Формат от сервера: "id|fullname|role"
        try {
            String[] parts = userData.split("\\|");
            if (parts.length >= 3) {
                CURRENT_USER_ID = Integer.parseInt(parts[0].trim());
                CURRENT_USER_NAME = parts[1].trim();
                CURRENT_USER_ROLE = parts[2].trim();

                Toast.makeText(this, "Вход выполнен: " + CURRENT_USER_NAME,
                        Toast.LENGTH_SHORT).show();

                // Переход к ленте мероприятий
                Intent intent = new Intent(LoginActivity.this, EventsActivity.class);
                startActivity(intent);
                finish(); // Закрыть экран входа
            } else {
                tvError.setText("Ошибка получения данных пользователя");
            }
        } catch (Exception e) {
            tvError.setText("Ошибка обработки данных");
        }
    }
}
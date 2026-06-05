package com.example.scheduleapp;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scheduleapp.network.AttendanceItem;
import com.example.scheduleapp.network.RetrofitClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventDetailsActivity extends AppCompatActivity {

    private int eventId;
    private String eventTitle;
    private String eventDate;
    private int unitId;

    private LinearLayout llActions;
    private TextView tvResult;
    private ListView lvStudents;
    private Button btnSaveAttendance, btnBack;

    private List<Map<String, Object>> studentsList;
    private Map<Integer, Boolean> attendanceMap = new HashMap<>();
    private int currentUserId;
    private String currentRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // ✅ Берём реальные данные из LoginActivity
        currentUserId = LoginActivity.CURRENT_USER_ID;
        currentRole = LoginActivity.CURRENT_USER_ROLE;

        System.out.println("=== EventDetailsActivity ===");
        System.out.println("currentUserId: " + currentUserId);
        System.out.println("currentRole: " + currentRole);

        eventId = getIntent().getIntExtra("eventId", 0);
        eventTitle = getIntent().getStringExtra("eventTitle");
        eventDate = getIntent().getStringExtra("eventDate");

        unitId = getIntent().getIntExtra("unitId", 0);

        System.out.println("DEBUG: Received unitId = " + unitId);

        ((TextView) findViewById(R.id.tvEventTitleHeader)).setText(eventTitle);
        ((TextView) findViewById(R.id.tvEventDateHeader)).setText("Дата: " + eventDate);

        llActions = findViewById(R.id.llActions);
        tvResult = findViewById(R.id.tvResult);
        lvStudents = findViewById(R.id.lvStudents);
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance);
        btnBack = findViewById(R.id.btnBack);

        findViewById(R.id.btnRegister).setOnClickListener(v -> registerForEvent());
        findViewById(R.id.btnMark).setOnClickListener(v -> showAttendanceForm());
        findViewById(R.id.btnDoc).setOnClickListener(v -> generateDocument());

        findViewById(R.id.btnReport).setOnClickListener(v -> generateReport());

        btnSaveAttendance.setOnClickListener(v -> saveAttendance());
        btnBack.setOnClickListener(v -> finish());
    }

    private void registerForEvent() {
        tvResult.setText("Запись на мероприятие...");
        RetrofitClient.getService().register(currentUserId, eventId, unitId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String msg = response.body() != null ? response.body().string()
                                    : response.errorBody().string();
                            tvResult.setText(response.isSuccessful() ? "УСПЕХ: " + msg : "ОТКАЗ: " + msg);
                        } catch (IOException e) { tvResult.setText("Ошибка: " + e.getMessage()); }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        tvResult.setText("Сеть: " + t.getMessage());
                    }
                });
    }

    private void showAttendanceForm() {
        if (!"starosta".equals(currentRole)) {
            tvResult.setText("Только староста может отмечать посещаемость");
            return;
        }
        llActions.setVisibility(View.GONE);
        lvStudents.setVisibility(View.VISIBLE);
        btnSaveAttendance.setVisibility(View.VISIBLE);
        tvResult.setText("Загрузка списка студентов...");

        RetrofitClient.getService().getEventStudents(eventId)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call,
                                           Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            studentsList = response.body();
                            attendanceMap.clear();
                            lvStudents.setAdapter(new AttendanceAdapter());
                            tvResult.setText("Отметьте присутствующих:");
                        } else {
                            tvResult.setText("Не удалось загрузить список");
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        tvResult.setText("Сеть: " + t.getMessage());
                    }
                });
    }

    private void saveAttendance() {
        if (studentsList == null || studentsList.isEmpty()) return;

        List<AttendanceItem> items = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (Map<String, Object> s : studentsList) {
            int id = ((Number) s.get("id")).intValue();
            items.add(new AttendanceItem(id, eventId, 1, today,
                    attendanceMap.getOrDefault(id, false)));
        }

        tvResult.setText("Сохранение...");
        RetrofitClient.getService().markAttendanceBatch(items)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String msg = response.body() != null ? response.body().string()
                                    : response.errorBody().string();
                            tvResult.setText(response.isSuccessful() ? "УСПЕХ: " + msg : "ОШИБКА: " + msg);
                        } catch (IOException e) { tvResult.setText("Ошибка: " + e.getMessage()); }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        tvResult.setText("Сеть: " + t.getMessage());
                    }
                });

        llActions.setVisibility(View.VISIBLE);
        lvStudents.setVisibility(View.GONE);
        btnSaveAttendance.setVisibility(View.GONE);
    }

    private void generateDocument() {
        tvResult.setText("Формирование документа...");
        RetrofitClient.getService().getDocument(eventId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String msg = response.body() != null ? response.body().string()
                                    : response.errorBody().string();
                            tvResult.setText(response.isSuccessful() ? msg : "ОШИБКА: " + msg);
                        } catch (IOException e) { tvResult.setText("Ошибка: " + e.getMessage()); }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        tvResult.setText("Сеть: " + t.getMessage());
                    }
                });
    }

    private void generateReport() {
        tvResult.setText("Формирование отчёта...");

        RetrofitClient.getService().getReport(eventId)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call,
                                           Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Map<String, Object>> reportData = response.body();

                            StringBuilder sb = new StringBuilder();
                            sb.append("ОТЧЁТ ПО ПОСЕЩАЕМОСТИ\n");
                            sb.append("=======================\n\n");
                            sb.append("Мероприятие: ").append(eventTitle).append("\n");
                            sb.append("Дата: ").append(eventDate).append("\n\n");

                            int presentCount = 0;
                            int absentCount = 0;
                            int noMarkCount = 0;

                            sb.append("СПИСОК СТУДЕНТОВ:\n");
                            sb.append("-----------------\n");

                            for (int i = 0; i < reportData.size(); i++) {
                                Map<String, Object> student = reportData.get(i);
                                String fullname = (String) student.get("fullname");
                                Boolean attended = (Boolean) student.get("attended");

                                sb.append((i + 1)).append(". ").append(fullname).append(" - ");

                                if (attended == null) {
                                    sb.append("НЕ ОТМЕЧЕН");
                                    noMarkCount++;
                                } else if (attended) {
                                    sb.append("ПРИСУТСТВОВАЛ");
                                    presentCount++;
                                } else {
                                    sb.append("ОТСУТСТВОВАЛ");
                                    absentCount++;
                                }
                                sb.append("\n");
                            }

                            sb.append("\n=======================\n");
                            sb.append("ИТОГИ:\n");
                            sb.append("Присутствовало: ").append(presentCount).append(" чел.\n");
                            sb.append("Отсутствовало: ").append(absentCount).append(" чел.\n");
                            if (noMarkCount > 0) {
                                sb.append("Не отмечено: ").append(noMarkCount).append(" чел.\n");
                            }
                            sb.append("Всего: ").append(reportData.size()).append(" чел.\n");

                            if (noMarkCount > 0) {
                                sb.append("\n⚠️ ВНИМАНИЕ: Не все студенты отмечены старостой!\n");
                            }

                            tvResult.setText(sb.toString());
                        } else {
                            try {
                                String error = response.errorBody() != null ?
                                        response.errorBody().string() : "Неизвестная ошибка";
                                tvResult.setText("Ошибка: " + error);
                            } catch (IOException e) {
                                tvResult.setText("Ошибка чтения ответа");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        tvResult.setText("Сеть: " + t.getMessage());
                    }
                });
    }

    private class AttendanceAdapter extends BaseAdapter {
        @Override public int getCount() { return studentsList.size(); }
        @Override public Object getItem(int p) { return studentsList.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_attendance, parent, false);
            }
            Map<String, Object> s = studentsList.get(position);
            int id = ((Number) s.get("id")).intValue();

            TextView tvName = convertView.findViewById(R.id.tvStudentName);
            CheckBox cb = convertView.findViewById(R.id.cbPresent);

            tvName.setText(s.get("fullname").toString());
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(attendanceMap.getOrDefault(id, false));
            cb.setOnCheckedChangeListener((b, isChecked) -> attendanceMap.put(id, isChecked));

            return convertView;
        }
    }
}
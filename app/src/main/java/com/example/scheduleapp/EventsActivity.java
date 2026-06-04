package com.example.scheduleapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scheduleapp.network.RetrofitClient;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventsActivity extends AppCompatActivity {

    private ListView lvEvents;
    private Button btnLogout;
    private List<Map<String, Object>> eventsList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        lvEvents = findViewById(R.id.lvEvents);
        btnLogout = findViewById(R.id.btnLogout);

        loadEvents();
        btnLogout.setOnClickListener(v->logout());
    }

    private void logout() {
        // 1. Очищаем данные текущего пользователя
        LoginActivity.CURRENT_USER_ID = -1;
        LoginActivity.CURRENT_USER_ROLE = "";
        LoginActivity.CURRENT_USER_NAME = "";

        // 2. Переходим на экран входа
        Intent intent = new Intent(EventsActivity.this, LoginActivity.class);

        // 3. Очищаем стек активностей, чтобы нельзя было вернуться назад кнопкой "Назад"
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish(); // Закрываем текущую активность
    }

    private void loadEvents() {
        RetrofitClient.getService().getEvents().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call,
                                   Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    eventsList = response.body();
                    lvEvents.setAdapter(new EventsAdapter());
                } else {
                    Toast.makeText(EventsActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(EventsActivity.this, "Сеть: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class EventsAdapter extends BaseAdapter {
        @Override public int getCount() { return eventsList.size(); }
        @Override public Object getItem(int p) { return eventsList.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_event, parent, false);
            }

            Map<String, Object> event = eventsList.get(position);
            ((TextView) convertView.findViewById(R.id.tvEventTitle)).setText(event.get("title").toString());
            ((TextView) convertView.findViewById(R.id.tvEventDate)).setText("Дата: " + event.get("date"));
            ((TextView) convertView.findViewById(R.id.tvEventDescription))
                    .setText(event.get("description") != null ? event.get("description").toString() : "");

            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(EventsActivity.this, EventDetailsActivity.class);
                intent.putExtra("eventId", ((Number) event.get("id")).intValue());
                intent.putExtra("eventTitle", event.get("title").toString());
                intent.putExtra("eventDate", event.get("date").toString());

                Object unitObj = event.get("unit_id");
                if (unitObj != null) {
                    intent.putExtra("unitId", ((Number) unitObj).intValue());
                } else {
                    intent.putExtra("unitId", 0); // Запасной вариант
                }

                startActivity(intent);
            });
            return convertView;
        }
    }
}
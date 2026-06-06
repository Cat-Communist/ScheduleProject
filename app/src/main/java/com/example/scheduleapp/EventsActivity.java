package com.example.scheduleapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
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
        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        LoginActivity.CURRENT_USER_ID = -1;
        LoginActivity.CURRENT_USER_ROLE = "";
        LoginActivity.CURRENT_USER_NAME = "";

        Intent intent = new Intent(EventsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

            ImageView iconView = convertView.findViewById(R.id.ivEventIcon);
            TextView tvTitle = convertView.findViewById(R.id.tvEventTitle);
            TextView tvDate = convertView.findViewById(R.id.tvEventDate);
            TextView tvDesc = convertView.findViewById(R.id.tvEventDescription);

            tvTitle.setText(event.get("title").toString());
            tvDate.setText("Дата: " + event.get("date"));
            tvDesc.setText(event.get("description") != null ? event.get("description").toString() : "");

            // Получаем unitId из данных мероприятия
            Object unitObj = event.get("unit_id");
            int unitId = 0;
            if (unitObj != null) {
                unitId = ((Number) unitObj).intValue();
            }

            // Устанавливаем иконку
            switch (unitId) {
                case 1: iconView.setImageResource(R.drawable.ic_it); break;
                case 2: iconView.setImageResource(R.drawable.ic_sport); break;
                case 3: iconView.setImageResource(R.drawable.ic_history); break;
                default: iconView.setImageResource(R.drawable.ic_default); break;
            }

            final int finalUnitId = unitId;

            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(EventsActivity.this, EventDetailsActivity.class);
                intent.putExtra("eventId", ((Number) event.get("id")).intValue());
                intent.putExtra("eventTitle", event.get("title").toString());
                intent.putExtra("eventDate", event.get("date").toString());
                intent.putExtra("unitId", finalUnitId);

                int iconResId;
                switch (finalUnitId) {
                    case 1: iconResId = R.drawable.ic_it; break;
                    case 2: iconResId = R.drawable.ic_sport; break;
                    case 3: iconResId = R.drawable.ic_history; break;
                    default: iconResId = R.drawable.ic_default; break;
                }
                intent.putExtra("iconResId", iconResId);

                startActivity(intent);
            });

            return convertView;
        }
    }
}
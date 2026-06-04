package com.example.scheduleapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Сразу переходим на экран входа
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
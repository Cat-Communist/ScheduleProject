package com.example.scheduleapp.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.GsonBuilder;

public class RetrofitClient {
    private static final String BASE_URL = "http://10.129.75.103:8080/api/";
    private static Retrofit retrofit = null;

    public static ApiService getService() {
        if (retrofit == null) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setLenient();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
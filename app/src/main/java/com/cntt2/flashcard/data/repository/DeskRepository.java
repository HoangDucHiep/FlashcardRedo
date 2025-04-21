package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.DeskDao;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.LearningSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeskRepository {
    private static final String TAG = "DeskRepository";
    private final ApiService apiService;
    private final Context context;

    public DeskRepository(Context context) {
        this.context = context;
        this.apiService = App.getInstance().getApiService();
    }

    public void insertDesk(DeskDto deskDto, Callback<DeskDto> callback) {
        Call<DeskDto> call = apiService.createDesk(deskDto);
        call.enqueue(new Callback<DeskDto>() {
            @Override
            public void onResponse(Call<DeskDto> call, Response<DeskDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Desk created - ID: " + response.body().getId());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to create desk: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to create desk: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<DeskDto> call, Throwable t) {
                Log.e(TAG, "Network error creating desk: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void updateDesk(String id, DeskDto deskDto, Callback<Void> callback) {
        Call<Void> call = apiService.updateDesk(id, deskDto);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Desk updated - ID: " + id);
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to update desk: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to update desk: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error updating desk: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void deleteDesk(String id, Callback<Void> callback) {
        Call<Void> call = apiService.deleteDesk(id);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error deleting desk: " + t.getMessage());
                callback.onFailure(call, t);
            }

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Desk deleted - ID: " + id);
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to delete desk: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to delete desk: " + response.message()));
                }
            }
        });
    }

    public void getAllDesks(Callback<List<DeskDto>> callback) {
        Call<List<DeskDto>> call = apiService.getUserDesks();
        call.enqueue(new Callback<List<DeskDto>>() {
            @Override
            public void onResponse(Call<List<DeskDto>> call, Response<List<DeskDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Fetched desks: " + response.body().size());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to fetch desks: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to fetch desks: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<List<DeskDto>> call, Throwable t) {
                Log.e(TAG, "Network error fetching desks: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }
}
package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.SessionDto;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LearningSessionRepository {
    private static final String TAG = "LearningSessionRepository";
    private final ApiService apiService;
    private final SimpleDateFormat dateFormat;

    public LearningSessionRepository(Context context) {
        this.apiService = App.getInstance().getApiService();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void insertSession(SessionDto sessionDto, Callback<SessionDto> callback) {
        validateSession(sessionDto);
        Call<SessionDto> call = apiService.createSession(sessionDto);
        call.enqueue(new Callback<SessionDto>() {
            @Override
            public void onResponse(Call<SessionDto> call, Response<SessionDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Session created - ID: " + response.body().getId());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to create session: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to create session: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<SessionDto> call, Throwable t) {
                Log.e(TAG, "Network error creating session: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void getSessionsByDeskId(String deskId, Callback<List<SessionDto>> callback) {
        if (deskId == null || deskId.isEmpty()) {
            callback.onFailure(null, new IllegalArgumentException("Desk ID cannot be null or empty"));
            return;
        }
        Call<List<SessionDto>> call = apiService.getSessionsByDeskId(deskId);
        call.enqueue(new Callback<List<SessionDto>>() {
            @Override
            public void onResponse(Call<List<SessionDto>> call, Response<List<SessionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Fetched sessions: " + response.body().size());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to fetch sessions: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to fetch sessions: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<List<SessionDto>> call, Throwable t) {
                Log.e(TAG, "Network error fetching sessions: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void deleteAllSessionsByDeskId(String deskId, Callback<Void> callback) {
        if (deskId == null || deskId.isEmpty()) {
            callback.onFailure(null, new IllegalArgumentException("Desk ID cannot be null or empty"));
            return;
        }
        Call<Void> call = apiService.deleteAllSessionsByDeskId(deskId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "All sessions deleted for deskId: " + deskId);
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to delete sessions: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to delete sessions: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error deleting sessions: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    private void validateSession(SessionDto session) {
        if (session.getDeskId() == null || session.getDeskId().isEmpty()) {
            throw new IllegalArgumentException("Invalid deskId: " + session.getDeskId());
        }
        if (session.getStartTime() == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        try {
            dateFormat.parse(session.getStartTime());
            if (session.getEndTime() != null) {
                dateFormat.parse(session.getEndTime());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + e.getMessage());
        }
        if (session.getCardsStudied() < 0) {
            throw new IllegalArgumentException("Cards studied cannot be negative");
        }
        if (session.getPerformance() < 0 || session.getPerformance() > 1) {
            throw new IllegalArgumentException("Performance must be between 0 and 1");
        }
    }
}
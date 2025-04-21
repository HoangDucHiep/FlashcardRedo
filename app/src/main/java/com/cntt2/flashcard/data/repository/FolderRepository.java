package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.FolderDao;
import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.GetFolderDto;
import com.cntt2.flashcard.data.remote.dto.NestedFolderDto;
import com.cntt2.flashcard.data.remote.dto.PostFolderDto;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;
import com.cntt2.flashcard.model.IdMapping;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FolderRepository {
    private static final String TAG = "FolderRepository";
    private final ApiService apiService;
    private final Context context;

    public FolderRepository(Context context) {
        this.context = context;
        this.apiService = App.getInstance().getApiService();
    }

    public void insertFolder(PostFolderDto folderDto, Callback<GetFolderDto> callback) {
        Call<GetFolderDto> call = apiService.createFolder(folderDto);
        call.enqueue(new Callback<GetFolderDto>() {
            @Override
            public void onResponse(Call<GetFolderDto> call, Response<GetFolderDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Folder created - ID: " + response.body().getId());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to create folder: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to create folder: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<GetFolderDto> call, Throwable t) {
                Log.e(TAG, "Network error creating folder: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void updateFolder(String id, PostFolderDto folderDto, Callback<Void> callback) {
        Call<Void> call = apiService.updateFolder(id, folderDto);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Folder updated - ID: " + id);
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to update folder: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to update folder: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error updating folder: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void deleteFolder(String id, Callback<Void> callback) {
        Call<Void> call = apiService.deleteFolder(id);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Folder deleted - ID: " + id);
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to delete folder: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to delete folder: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error deleting folder: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void getNestedFolders(Callback<List<NestedFolderDto>> callback) {
        Call<List<NestedFolderDto>> call = apiService.getNestedFolders();
        call.enqueue(new Callback<List<NestedFolderDto>>() {
            @Override
            public void onResponse(Call<List<NestedFolderDto>> call, Response<List<NestedFolderDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Fetched nested folders: " + response.body().size());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to fetch nested folders: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to fetch nested folders: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<List<NestedFolderDto>> call, Throwable t) {
                Log.e(TAG, "Network error fetching nested folders: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }
}
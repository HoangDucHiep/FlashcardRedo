package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.CardDto;
import com.cntt2.flashcard.data.remote.dto.ImageDto;
import com.cntt2.flashcard.utils.ImageManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardRepository {
    private static final String TAG = "CardRepository";
    private final ApiService apiService;
    private final Context context;
    private final SimpleDateFormat dateFormat;

    public CardRepository(Context context) {
        this.context = context;
        this.apiService = App.getInstance().getApiService();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void insertCard(CardDto cardDto, List<String> localImagePaths, Callback<CardDto> callback) {
        uploadImagesAndCreateCard(cardDto, localImagePaths, callback);
    }

    private void uploadImagesAndCreateCard(CardDto cardDto, List<String> localImagePaths, Callback<CardDto> callback) {
        if (localImagePaths.isEmpty()) {
            createCard(cardDto, callback);
            return;
        }

        List<String> serverImagePaths = new ArrayList<>();
        int[] uploadCount = {0};

        for (String localPath : localImagePaths) {
            File file = new File(localPath);
            if (!file.exists()) {
                Log.w(TAG, "Image file does not exist: " + localPath);
                continue;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            apiService.uploadImage(body).enqueue(new Callback<ImageDto>() {
                @Override
                public void onResponse(Call<ImageDto> call, Response<ImageDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        serverImagePaths.add(response.body().getUrl());
                        Log.d(TAG, "Uploaded image: " + response.body().getUrl());
                    } else {
                        Log.e(TAG, "Failed to upload image: " + response.message());
                    }

                    uploadCount[0]++;
                    if (uploadCount[0] == localImagePaths.size()) {
                        Log.d(TAG, "Local paths: " + localImagePaths);
                        Log.d(TAG, "Server URLs: " + serverImagePaths);
                        cardDto.setFront(replaceImagePaths(cardDto.getFront(), localImagePaths, serverImagePaths));
                        cardDto.setBack(replaceImagePaths(cardDto.getBack(), localImagePaths, serverImagePaths));
                        createCard(cardDto, callback);
                        ImageManager.deleteImageFiles(new HashSet<>(localImagePaths), context);
                    }
                }

                @Override
                public void onFailure(Call<ImageDto> call, Throwable t) {
                    Log.e(TAG, "Network error uploading image: " + t.getMessage());
                    uploadCount[0]++;
                    if (uploadCount[0] == localImagePaths.size()) {
                        cardDto.setFront(replaceImagePaths(cardDto.getFront(), localImagePaths, serverImagePaths));
                        cardDto.setBack(replaceImagePaths(cardDto.getBack(), localImagePaths, serverImagePaths));
                        createCard(cardDto, callback);
                        ImageManager.deleteImageFiles(new HashSet<>(localImagePaths), context);
                    }
                }
            });
        }
    }

    private void createCard(CardDto cardDto, Callback<CardDto> callback) {
        Log.d(TAG, "Creating card with front: " + cardDto.getFront());
        Log.d(TAG, "Creating card with back: " + cardDto.getBack());
        Call<CardDto> call = apiService.createCard(cardDto);
        call.enqueue(new Callback<CardDto>() {
            @Override
            public void onResponse(Call<CardDto> call, Response<CardDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Card created - ID: " + response.body().getId());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to create card: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to create card: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<CardDto> call, Throwable t) {
                Log.e(TAG, "Network error creating card: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void updateCard(String id, CardDto cardDto, List<String> localImagePaths, Callback<CardDto> callback) {
        uploadImagesAndUpdateCard(id, cardDto, localImagePaths, callback);
    }

    private void uploadImagesAndUpdateCard(String id, CardDto cardDto, List<String> localImagePaths, Callback<CardDto> callback) {
        if (localImagePaths.isEmpty()) {
            updateCard(id, cardDto, callback);
            return;
        }

        List<String> serverImagePaths = new ArrayList<>();
        int[] uploadCount = {0};

        for (String localPath : localImagePaths) {
            File file = new File(localPath);
            if (!file.exists()) {
                Log.w(TAG, "Image file does not exist: " + localPath);
                continue;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            apiService.uploadImage(body).enqueue(new Callback<ImageDto>() {
                @Override
                public void onResponse(Call<ImageDto> call, Response<ImageDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        serverImagePaths.add(response.body().getUrl());
                        Log.d(TAG, "Uploaded image: " + response.body().getUrl());
                    } else {
                        Log.e(TAG, "Failed to upload image: " + response.message());
                    }

                    uploadCount[0]++;
                    if (uploadCount[0] == localImagePaths.size()) {
                        Log.d(TAG, "Local paths: " + localImagePaths);
                        Log.d(TAG, "Server URLs: " + serverImagePaths);
                        cardDto.setFront(replaceImagePaths(cardDto.getFront(), localImagePaths, serverImagePaths));
                        cardDto.setBack(replaceImagePaths(cardDto.getBack(), localImagePaths, serverImagePaths));
                        updateCard(id, cardDto, callback);
                        ImageManager.deleteImageFiles(new HashSet<>(localImagePaths), context);
                    }
                }

                @Override
                public void onFailure(Call<ImageDto> call, Throwable t) {
                    Log.e(TAG, "Network error uploading image: " + t.getMessage());
                    uploadCount[0]++;
                    if (uploadCount[0] == localImagePaths.size()) {
                        cardDto.setFront(replaceImagePaths(cardDto.getFront(), localImagePaths, serverImagePaths));
                        cardDto.setBack(replaceImagePaths(cardDto.getBack(), localImagePaths, serverImagePaths));
                        updateCard(id, cardDto, callback);
                        ImageManager.deleteImageFiles(new HashSet<>(localImagePaths), context);
                    }
                }
            });
        }
    }

    private void updateCard(String id, CardDto cardDto, Callback<CardDto> callback) {
        Log.d(TAG, "Updating card with front: " + cardDto.getFront());
        Log.d(TAG, "Updating card with back: " + cardDto.getBack());
        Call<CardDto> call = apiService.updateCard(id, cardDto);
        call.enqueue(new Callback<CardDto>() {
            @Override
            public void onResponse(Call<CardDto> call, Response<CardDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Card updated - ID: " + id);
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to update card: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to update card: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<CardDto> call, Throwable t) {
                Log.e(TAG, "Network error updating card: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void deleteCard(String id, Callback<Void> callback) {
        Call<Void> call = apiService.deleteCard(id);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Card deleted - ID: " + id);
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to delete card: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to delete card: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error deleting card: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void getCardsByDeskId(String deskId, Callback<List<CardDto>> callback) {
        Call<List<CardDto>> call = apiService.getCardsByDeskId(deskId);
        call.enqueue(new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Fetched cards: " + response.body().size());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to fetch cards: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to fetch cards: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                Log.e(TAG, "Network error fetching cards: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void getNewCards(String deskId, Callback<List<CardDto>> callback) {
        Call<List<CardDto>> call = apiService.getNewCards(deskId);
        call.enqueue(new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Fetched new cards: " + response.body().size());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to fetch new cards: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to fetch new cards: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                Log.e(TAG, "Network error fetching new cards: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void getCardsToReview(String deskId, Callback<List<CardDto>> callback) {
        String today = dateFormat.format(new Date());
        Call<List<CardDto>> call = apiService.getCardsDueToday(deskId, today);
        call.enqueue(new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Fetched cards to review: " + response.body().size());
                    callback.onResponse(call, response);
                } else {
                    Log.e(TAG, "Failed to fetch cards to review: " + response.message());
                    callback.onFailure(call, new Throwable("Failed to fetch cards to review: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                Log.e(TAG, "Network error fetching cards to review: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    private String replaceImagePaths(String html, List<String> localPaths, List<String> serverUrls) {
        if (html == null || localPaths.isEmpty() || serverUrls.isEmpty()) {
            Log.d(TAG, "No replacement needed: html=" + (html == null ? "null" : "not null") +
                    ", localPaths=" + localPaths.size() + ", serverUrls=" + serverUrls.size());
            return html;
        }

        String result = html;
        for (int i = 0; i < localPaths.size() && i < serverUrls.size(); i++) {
            String localPath = localPaths.get(i);
            String serverUrl = serverUrls.get(i);
            String fileName = new File(localPath).getName();

            // Replace content:// URIs
            String contentUriPattern = "content://com\\.cntt2\\.flashcard\\.fileprovider/images/" + fileName.replaceAll("[\\W]", "\\\\$0");
            result = result.replaceAll(contentUriPattern, serverUrl);

            // Replace file:// URIs
            String fileUriPattern = "file://" + localPath.replaceAll("[\\W]", "\\\\$0");
            result = result.replaceAll(fileUriPattern, serverUrl);

            Log.d(TAG, "Replacing local path: " + localPath + " with server URL: " + serverUrl);
            Log.d(TAG, "Updated HTML: " + result);
        }
        return result;
    }
}
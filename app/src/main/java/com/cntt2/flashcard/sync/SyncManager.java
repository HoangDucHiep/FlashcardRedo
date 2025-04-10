package com.cntt2.flashcard.sync;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.remote.ApiClient;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.GetFolderDto;
import com.cntt2.flashcard.data.remote.dto.PostFolderDto;
import com.cntt2.flashcard.data.repository.FolderRepository;
import com.cntt2.flashcard.model.Folder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final FolderRepository folderRepository;
    private final ApiService apiService;
    private final SimpleDateFormat dateFormat; // Định dạng gửi lên API
    private final SimpleDateFormat localDateFormat; // Định dạng từ local
    private final SimpleDateFormat serverDateFormat; // Định dạng từ server

    public SyncManager(Context context) {
        this.folderRepository = App.getInstance().getFolderRepository();
        this.apiService = ApiClient.getApiService();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        this.localDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.serverDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    public void syncFolders(final SyncCallback callback) {
        Log.d(TAG, "Starting folder sync...");
        pullFoldersFromServer(new SyncCallback() {
            @Override
            public void onSuccess() {
                pushFoldersToServer(callback);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    private void pullFoldersFromServer(final SyncCallback callback) {
        Log.d(TAG, "Pulling folders from server...");
        apiService.getUserFolders().enqueue(new Callback<List<GetFolderDto>>() {
            @Override
            public void onResponse(Call<List<GetFolderDto>> call, Response<List<GetFolderDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully pulled " + response.body().size() + " folders from server");
                    List<GetFolderDto> serverFolders = response.body();
                    for (GetFolderDto serverFolder : serverFolders) {
                        // Kiểm tra serverId có null không
                        if (serverFolder.getId() == null) {
                            Log.e(TAG, "Server folder has null ID, skipping: " + serverFolder.getName());
                            continue; // Bỏ qua folder này
                        }

                        Integer localId = folderRepository.getLocalIdByServerId(serverFolder.getId());
                        Folder localFolder = null;
                        if (localId != null) {
                            List<Folder> allFolders = folderRepository.getAllFolders();
                            for (Folder f : allFolders) {
                                if (f.getId() == localId) {
                                    localFolder = f;
                                    break;
                                }
                            }
                        }

                        try {
                            // Sử dụng trực tiếp Date từ serverFolder
                            Date serverLastModified = serverFolder.getLastModified();
                            if (serverLastModified == null) {
                                Log.e(TAG, "Server folder has null LastModified, skipping: " + serverFolder.getName());
                                continue; // Bỏ qua folder này nếu LastModified là null
                            }

                            if (localFolder == null) {
                                Folder newFolder = new Folder();
                                newFolder.setServerId(serverFolder.getId());
                                newFolder.setParentFolderId(serverFolder.getParentFolderId() != null ?
                                        folderRepository.getLocalIdByServerId(serverFolder.getParentFolderId()) : null);
                                newFolder.setName(serverFolder.getName());
                                newFolder.setCreatedAt(dateFormat.format(serverFolder.getCreatedAt()));
                                newFolder.setLastModified(dateFormat.format(serverLastModified));
                                newFolder.setSyncStatus("synced");
                                long newLocalId = folderRepository.insertFolder(newFolder);
                                folderRepository.insertIdMapping(newLocalId, serverFolder.getId());
                                Log.d(TAG, "Inserted new folder with localId: " + newLocalId);
                            } else {
                                Date localLastModified = localDateFormat.parse(localFolder.getLastModified());
                                if (serverLastModified.after(localLastModified) && !localFolder.getSyncStatus().equals("pending_update")) {
                                    localFolder.setName(serverFolder.getName());
                                    localFolder.setParentFolderId(serverFolder.getParentFolderId() != null ?
                                            folderRepository.getLocalIdByServerId(serverFolder.getParentFolderId()) : null);
                                    localFolder.setLastModified(dateFormat.format(serverLastModified));
                                    localFolder.setSyncStatus("synced");
                                    folderRepository.updateFolder(localFolder);
                                    Log.d(TAG, "Updated folder with localId: " + localFolder.getId());
                                }
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing date: " + e.getMessage());
                        }
                    }
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to pull folders: " + response.code() + " - " + response.message());
                    try {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                    callback.onFailure("Failed to pull folders: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<GetFolderDto>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void pushFoldersToServer(final SyncCallback callback) {
        Log.d(TAG, "Pushing folders to server...");
        List<Folder> pendingCreateFolders = folderRepository.getPendingFolders("pending_create");
        List<Folder> pendingUpdateFolders = folderRepository.getPendingFolders("pending_update");
        List<Folder> pendingDeleteFolders = folderRepository.getPendingFolders("pending_delete");

        Log.d(TAG, "Pending create: " + pendingCreateFolders.size() + ", update: " + pendingUpdateFolders.size() + ", delete: " + pendingDeleteFolders.size());

        for (Folder localFolder : pendingCreateFolders) {
            createFolderOnServer(localFolder, callback);
        }

        for (Folder localFolder : pendingUpdateFolders) {
            updateFolderOnServer(localFolder, callback);
        }

        for (Folder localFolder : pendingDeleteFolders) {
            deleteFolderOnServer(localFolder, callback);
        }

        callback.onSuccess();
    }

    private void createFolderOnServer(final Folder localFolder, final SyncCallback callback) {
        Log.d(TAG, "Creating folder on server: " + localFolder.getName());

        // Create PostFolderDto object
        PostFolderDto postFolderDto = new PostFolderDto();
        postFolderDto.setName(localFolder.getName()); // Set the folder name (e.g., "Sec test")
        postFolderDto.setParentFolderId(localFolder.getParentFolderId() != null ?
                getServerIdFromLocalId(localFolder.getParentFolderId()) : null);

        // Parse and set dates
        try {
            Date createdAt = localDateFormat.parse(localFolder.getCreatedAt());
            Date lastModified = localDateFormat.parse(localFolder.getLastModified());
            postFolderDto.setCreatedAt(createdAt);
            postFolderDto.setLastModified(lastModified);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for folder " + localFolder.getName() + ": " + e.getMessage());
            callback.onFailure("Error parsing date: " + e.getMessage());
            return;
        }

        // Make the API call
        apiService.createFolder(postFolderDto).enqueue(new Callback<GetFolderDto>() { // Phản hồi là GetFolderDto
            @Override
            public void onResponse(Call<GetFolderDto> call, Response<GetFolderDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GetFolderDto serverFolder = response.body();
                    localFolder.setServerId(serverFolder.getId());
                    folderRepository.updateSyncStatus(localFolder.getId(), "synced");
                    folderRepository.insertIdMapping(localFolder.getId(), serverFolder.getId());
                    Log.d(TAG, "Created folder on server with serverId: " + serverFolder.getId());
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to create folder: " + response.code() + " - " + response.message());
                    try {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                    callback.onFailure("Failed to create folder: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<GetFolderDto> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void updateFolderOnServer(final Folder localFolder, final SyncCallback callback) {
        Log.d(TAG, "Updating folder on server: " + localFolder.getName());
        PostFolderDto postFolderDto = new PostFolderDto(); // Sử dụng PostFolderDto cho update
        postFolderDto.setName(localFolder.getName());
        postFolderDto.setParentFolderId(localFolder.getParentFolderId() != null ?
                getServerIdFromLocalId(localFolder.getParentFolderId()) : null);

        try {
            Date createdAt = localDateFormat.parse(localFolder.getCreatedAt());
            Date lastModified = localDateFormat.parse(localFolder.getLastModified());
            postFolderDto.setCreatedAt(createdAt);
            postFolderDto.setLastModified(lastModified);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for folder " + localFolder.getName() + ": " + e.getMessage());
            callback.onFailure("Error parsing date: " + e.getMessage());
            return;
        }

        apiService.updateFolder(localFolder.getServerId(), postFolderDto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    folderRepository.updateSyncStatus(localFolder.getId(), "synced");
                    Log.d(TAG, "Updated folder on server with serverId: " + localFolder.getServerId());
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to update folder: " + response.code() + " - " + response.message());
                    try {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                    callback.onFailure("Failed to update folder: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void deleteFolderOnServer(final Folder localFolder, final SyncCallback callback) {
        Log.d(TAG, "Deleting folder on server: " + localFolder.getName());
        if (localFolder.getServerId() == null) {
            folderRepository.deleteFolderConfirmed(localFolder.getId());
            Log.d(TAG, "Deleted local folder with no serverId: " + localFolder.getId());
            callback.onSuccess();
            return;
        }

        apiService.deleteFolder(localFolder.getServerId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    folderRepository.deleteFolderConfirmed(localFolder.getId());
                    Log.d(TAG, "Deleted folder on server with serverId: " + localFolder.getServerId());
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to delete folder: " + response.code() + " - " + response.message());
                    try {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                    callback.onFailure("Failed to delete folder: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private String getServerIdFromLocalId(int localId) {
        List<Folder> allFolders = folderRepository.getAllFolders();
        for (Folder folder : allFolders) {
            if (folder.getId() == localId) {
                return folder.getServerId();
            }
        }
        return null;
    }

    private Date parseDate(String dateStr) {
        try {
            return localDateFormat.parse(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            return new Date();
        }
    }

    public interface SyncCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
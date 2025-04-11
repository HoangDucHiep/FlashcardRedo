package com.cntt2.flashcard.sync;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.remote.ApiClient;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.remote.dto.GetFolderDto;
import com.cntt2.flashcard.data.remote.dto.PostFolderDto;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.data.repository.FolderRepository;
import com.cntt2.flashcard.data.repository.IdMappingRepository;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;
import com.cntt2.flashcard.model.IdMapping;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final FolderRepository folderRepository;
    private final DeskRepository deskRepository;
    private final ApiService apiService;
    private final SimpleDateFormat dateFormat; // Định dạng gửi lên API
    private final SimpleDateFormat localDateFormat; // Định dạng từ local
    private final SimpleDateFormat serverDateFormat; // Định dạng từ server
    private final IdMappingRepository idMappingRepository;

    public SyncManager(Context context) {
        this.folderRepository = App.getInstance().getFolderRepository();
        this.deskRepository = App.getInstance().getDeskRepository();
        this.apiService = ApiClient.getApiService();
        idMappingRepository = new IdMappingRepository(context);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Đặt múi giờ là UTC
        this.localDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        this.localDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Đặt múi giờ là UTC
        this.serverDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.getDefault());
        this.serverDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Đặt múi giờ là UTC
    }


    // FOLDER SYNC SECTION
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
                        if (serverFolder.getId() == null) {
                            Log.e(TAG, "Server folder has null ID, skipping: " + serverFolder.getName());
                            continue;
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
                            Date serverLastModified = serverFolder.getLastModified();
                            if (serverLastModified == null) {
                                Log.e(TAG, "Server folder has null LastModified, skipping: " + serverFolder.getName());
                                continue;
                            }

                            if (localFolder == null) {
                                if (folderRepository.getLocalIdByServerId(serverFolder.getId()) != null) {
                                    Log.d(TAG, "Folder with serverId " + serverFolder.getId() + " already exists, skipping");
                                    continue;
                                }

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
                                } else if (localLastModified.after(serverLastModified) && !localFolder.getSyncStatus().equals("pending_update")) {
                                    // Nếu local mới hơn server, đánh dấu folder là pending_update
                                    localFolder.setSyncStatus("pending_update");
                                    folderRepository.updateFolder(localFolder);
                                    Log.d(TAG, "Local folder is newer, marked as pending_update: " + localFolder.getId());
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

        int totalPending = pendingCreateFolders.size() + pendingUpdateFolders.size() + pendingDeleteFolders.size();
        if (totalPending == 0) {
            // Kiểm tra nếu còn folder pending_update sau khi xử lý hết
            if (folderRepository.getPendingFolders("pending_update").isEmpty()) {
                callback.onSuccess();
            } else {
                Log.d(TAG, "Folders still pending_update, triggering another sync...");
                syncFolders(callback); // Gọi lại syncFolders để xử lý pending_update
            }
            return;
        }

        final int[] completedTasks = {0};

        for (Folder localFolder : pendingCreateFolders) {
            if (localFolder.getServerId() != null) {
                Log.d(TAG, "Folder " + localFolder.getName() + " already has serverId " + localFolder.getServerId() + ", skipping create");
                folderRepository.updateSyncStatus(localFolder.getId(), "synced");
                completedTasks[0]++;
                if (completedTasks[0] == totalPending) {
                    checkAndResync(callback);
                }
                continue;
            }
            createFolderOnServer(localFolder, new SyncCallback() {
                @Override
                public void onSuccess() {
                    completedTasks[0]++;
                    if (completedTasks[0] == totalPending) {
                        checkAndResync(callback);
                    }
                }

                @Override
                public void onFailure(String error) {
                    callback.onFailure(error);
                }
            });
        }

        for (Folder localFolder : pendingUpdateFolders) {
            updateFolderOnServer(localFolder, new SyncCallback() {
                @Override
                public void onSuccess() {
                    completedTasks[0]++;
                    if (completedTasks[0] == totalPending) {
                        checkAndResync(callback);
                    }
                }

                @Override
                public void onFailure(String error) {
                    callback.onFailure(error);
                }
            });
        }

        for (Folder localFolder : pendingDeleteFolders) {
            deleteFolderOnServer(localFolder, new SyncCallback() {
                @Override
                public void onSuccess() {
                    completedTasks[0]++;
                    if (completedTasks[0] == totalPending) {
                        checkAndResync(callback);
                    }
                }

                @Override
                public void onFailure(String error) {
                    callback.onFailure(error);
                }
            });
        }

        callback.onSuccess();
    }

    private void checkAndResync(SyncCallback callback) {
        List<Folder> remainingPendingUpdates = folderRepository.getPendingFolders("pending_update");
        if (remainingPendingUpdates.isEmpty()) {
            callback.onSuccess();
        } else {
            Log.d(TAG, "Found " + remainingPendingUpdates.size() + " folders still pending_update, triggering another sync...");
            syncFolders(callback);
        }
    }

    private void createFolderOnServer(final Folder localFolder, final SyncCallback callback) {
        Log.d(TAG, "Creating folder on server: " + localFolder.getName());

        PostFolderDto postFolderDto = new PostFolderDto();
        postFolderDto.setName(localFolder.getName());
        postFolderDto.setParentFolderId(localFolder.getParentFolderId() != null ?
                idMappingRepository.getServerIdByLocalId(localFolder.getParentFolderId(), "folder") : null);

        Date createdAt;
        Date lastModified;
        try {
            // Try parsing with the expected format
            createdAt = localDateFormat.parse(localFolder.getCreatedAt());
            lastModified = localDateFormat.parse(localFolder.getLastModified());
        } catch (ParseException e) {
            // Fallback to parsing "yyyy-MM-dd" format
            SimpleDateFormat fallbackFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            fallbackFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                createdAt = fallbackFormat.parse(localFolder.getCreatedAt());
                lastModified = fallbackFormat.parse(localFolder.getLastModified());
                // Update local folder to include time component for consistency
                localFolder.setCreatedAt(localDateFormat.format(createdAt));
                localFolder.setLastModified(localDateFormat.format(lastModified));
                folderRepository.updateFolder(localFolder);
                Log.d(TAG, "Updated folder " + localFolder.getName() + " with full date format");
            } catch (ParseException ex) {
                Log.e(TAG, "Error parsing date for folder " + localFolder.getName() + ": " + ex.getMessage());
                callback.onFailure("Error parsing date: " + ex.getMessage());
                return;
            }
        }

        postFolderDto.setCreatedAt(createdAt);
        postFolderDto.setLastModified(lastModified);

        apiService.createFolder(postFolderDto).enqueue(new Callback<GetFolderDto>() {
            @Override
            public void onResponse(Call<GetFolderDto> call, Response<GetFolderDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GetFolderDto serverFolder = response.body();
                    localFolder.setServerId(serverFolder.getId());
                    localFolder.setLastModified(dateFormat.format(serverFolder.getLastModified())); // Cập nhật LastModified từ server
                    if (localFolder.getParentFolderId() != null && postFolderDto.getParentFolderId() == null)
                        folderRepository.updateSyncStatus(localFolder.getId(), "pending_update");
                    else {
                        folderRepository.updateSyncStatus(localFolder.getId(), "synced");
                    }


                    Integer existingLocalId = folderRepository.getLocalIdByServerId(serverFolder.getId());
                    if (existingLocalId == null) {
                        String existingServerId = idMappingRepository.getServerIdByLocalId(localFolder.getId(), "folder");
                        if (existingServerId != null) {
                            IdMapping mapping = new IdMapping(localFolder.getId(), serverFolder.getId(), "folder");
                            idMappingRepository.updateIdMapping(mapping);
                            Log.d(TAG, "Updated id_mapping for localId: " + localFolder.getId() + " with new serverId: " + serverFolder.getId());
                        } else {
                            folderRepository.insertIdMapping(localFolder.getId(), serverFolder.getId());
                        }
                    } else {
                        Log.d(TAG, "Mapping already exists for serverId: " + serverFolder.getId() + ", skipping insert");
                    }

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

        String serverId = idMappingRepository.getServerIdByLocalId(localFolder.getId(), "folder");

        PostFolderDto postFolderDto = new PostFolderDto();
        postFolderDto.setName(localFolder.getName());
        postFolderDto.setParentFolderId(localFolder.getParentFolderId() != null ?
                idMappingRepository.getServerIdByLocalId(localFolder.getParentFolderId(), "folder") : null);

        Date createdAt;
        Date lastModified;
        try {
            createdAt = localDateFormat.parse(localFolder.getCreatedAt());
            lastModified = localDateFormat.parse(localFolder.getLastModified());
        } catch (ParseException e) {
            // Fallback to parsing "yyyy-MM-dd" format
            SimpleDateFormat fallbackFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            fallbackFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                createdAt = fallbackFormat.parse(localFolder.getCreatedAt());
                lastModified = fallbackFormat.parse(localFolder.getLastModified());
                // Update local folder to include time component for consistency
                localFolder.setCreatedAt(localDateFormat.format(createdAt));
                localFolder.setLastModified(localDateFormat.format(lastModified));
                folderRepository.updateFolder(localFolder);
                Log.d(TAG, "Updated folder " + localFolder.getName() + " with full date format");
            } catch (ParseException ex) {
                Log.e(TAG, "Error parsing date for folder " + localFolder.getName() + ": " + ex.getMessage());
                callback.onFailure("Error parsing date: " + ex.getMessage());
                return;
            }
        }

        postFolderDto.setCreatedAt(createdAt);
        postFolderDto.setLastModified(lastModified);


        apiService.updateFolder(serverId, postFolderDto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    folderRepository.updateSyncStatus(localFolder.getId(), "synced");
                    Log.d(TAG, "Updated folder on server with serverId: " + serverId);
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
        String serverId = idMappingRepository.getServerIdByLocalId(localFolder.getId(), "folder");
        Log.d(TAG, "Deleting folder on server: " + localFolder.getName());
        if (serverId == null) {
            folderRepository.deleteFolderConfirmed(localFolder.getId());
            Log.d(TAG, "Deleted local folder with no serverId: " + localFolder.getId());
            callback.onSuccess();
            return;
        }
        apiService.deleteFolder(serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    folderRepository.deleteFolderConfirmed(localFolder.getId());
                    Log.d(TAG, "Deleted folder on server with serverId: " + serverId);
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


    // DESK SYNC SECTION
    public void syncDesks(final SyncCallback callback) {
        Log.d(TAG, "Starting desk sync...");
        pullDesksFromServer(new SyncCallback() {
            @Override
            public void onSuccess() {
                pushDesksToServer(callback);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    private void pullDesksFromServer(final SyncCallback callback) {
        Log.d(TAG, "Pulling desks from server...");
        apiService.getUserDesks().enqueue(new Callback<List<DeskDto>>() {
            @Override
            public void onResponse(Call<List<DeskDto>> call, Response<List<DeskDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully pulled " + response.body().size() + " desks from server");
                    List<DeskDto> serverDesks = response.body();
                    for (DeskDto serverDesk : serverDesks) {
                        if (serverDesk.getId() == null) {
                            Log.e(TAG, "Server desk has null ID, skipping: " + serverDesk.getName());
                            continue;
                        }

                        Integer localId = deskRepository.getLocalIdByServerId(serverDesk.getId());
                        Desk localDesk = localId != null ? deskRepository.getDeskById(localId) : null;

                        try {
                            Date serverLastModified = serverDesk.getLastModified();
                            if (serverLastModified == null) {
                                Log.e(TAG, "Server desk has null LastModified, skipping: " + serverDesk.getName());
                                continue;
                            }

                            if (localDesk == null) {
                                if (deskRepository.getLocalIdByServerId(serverDesk.getId()) != null) {
                                    Log.d(TAG, "Desk with serverId " + serverDesk.getId() + " already exists, skipping");
                                    continue;
                                }

                                Desk newDesk = new Desk();
                                newDesk.setServerId(serverDesk.getId());
                                newDesk.setName(serverDesk.getName());
                                newDesk.setFolderId(serverDesk.getFolderId() != null ?
                                        folderRepository.getLocalIdByServerId(serverDesk.getFolderId()) : null);
                                newDesk.setPublic(serverDesk.isPublic());
                                newDesk.setCreatedAt(dateFormat.format(serverDesk.getCreatedAt()));
                                newDesk.setLastModified(dateFormat.format(serverLastModified));
                                newDesk.setSyncStatus("synced");
                                long newLocalId = deskRepository.insertDesk(newDesk);
                                deskRepository.insertIdMapping(newLocalId, serverDesk.getId());
                                Log.d(TAG, "Inserted new desk with localId: " + newLocalId);
                            } else {
                                Date localLastModified = localDateFormat.parse(localDesk.getLastModified());
                                if (serverLastModified.after(localLastModified) && !localDesk.getSyncStatus().equals("pending_update")) {
                                    localDesk.setName(serverDesk.getName());
                                    localDesk.setFolderId(serverDesk.getFolderId() != null ?
                                            folderRepository.getLocalIdByServerId(serverDesk.getFolderId()) : null);
                                    localDesk.setPublic(serverDesk.isPublic());
                                    localDesk.setLastModified(dateFormat.format(serverLastModified));
                                    localDesk.setSyncStatus("synced");
                                    deskRepository.updateDesk(localDesk);
                                    Log.d(TAG, "Updated desk with localId: " + localDesk.getId());
                                } else if (localLastModified.after(serverLastModified) && !localDesk.getSyncStatus().equals("pending_update")) {
                                    localDesk.setSyncStatus("pending_update");
                                    deskRepository.updateDesk(localDesk);
                                    Log.d(TAG, "Local desk is newer, marked as pending_update: " + localDesk.getId());
                                }
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing date: " + e.getMessage());
                        }
                    }
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to pull desks: " + response.code() + " - " + response.message());
                    callback.onFailure("Failed to pull desks: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<DeskDto>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void pushDesksToServer(final SyncCallback callback) {
        Log.d(TAG, "Pushing desks to server...");
        List<Desk> pendingCreateDesks = deskRepository.getPendingDesks("pending_create");
        List<Desk> pendingUpdateDesks = deskRepository.getPendingDesks("pending_update");
        List<Desk> pendingDeleteDesks = deskRepository.getPendingDesks("pending_delete");

        Log.d(TAG, "Pending create: " + pendingCreateDesks.size() + ", update: " + pendingUpdateDesks.size() + ", delete: " + pendingDeleteDesks.size());

        int totalPending = pendingCreateDesks.size() + pendingUpdateDesks.size() + pendingDeleteDesks.size();
        if (totalPending == 0) {
            // Kiểm tra nếu còn desk pending_update sau khi xử lý hết
            if (deskRepository.getPendingDesks("pending_update").isEmpty()) {
                callback.onSuccess();
            } else {
                Log.d(TAG, "Desks still pending_update, triggering another sync...");
                syncDesks(callback); // Gọi lại syncDesks để xử lý pending_update
            }
            return;
        }

        final int[] completedTasks = {0};

        for (Desk localDesk : pendingCreateDesks) {
            if (localDesk.getServerId() != null) {
                Log.d(TAG, "Desk " + localDesk.getName() + " already has serverId " + localDesk.getServerId() + ", skipping create");
                deskRepository.updateSyncStatus(localDesk.getId(), "synced");
                completedTasks[0]++;
                if (completedTasks[0] == totalPending) {
                    checkAndResyncDesks(callback);
                }
                continue;
            }
            createDeskOnServer(localDesk, new SyncCallback() {
                @Override
                public void onSuccess() {
                    completedTasks[0]++;
                    if (completedTasks[0] == totalPending) {
                        checkAndResyncDesks(callback);
                    }
                }

                @Override
                public void onFailure(String error) {
                    callback.onFailure(error);
                }
            });
        }

        for (Desk localDesk : pendingUpdateDesks) {
            updateDeskOnServer(localDesk, new SyncCallback() {
                @Override
                public void onSuccess() {
                    completedTasks[0]++;
                    if (completedTasks[0] == totalPending) {
                        checkAndResyncDesks(callback);
                    }
                }

                @Override
                public void onFailure(String error) {
                    callback.onFailure(error);
                }
            });
        }

        for (Desk localDesk : pendingDeleteDesks) {
            deleteDeskOnServer(localDesk, new SyncCallback() {
                @Override
                public void onSuccess() {
                    completedTasks[0]++;
                    if (completedTasks[0] == totalPending) {
                        checkAndResyncDesks(callback);
                    }
                }

                @Override
                public void onFailure(String error) {
                    callback.onFailure(error);
                }
            });
        }
    }

    // Helper method để kiểm tra và gọi lại sync desks nếu cần
    private void checkAndResyncDesks(SyncCallback callback) {
        List<Desk> remainingPendingUpdates = deskRepository.getPendingDesks("pending_update");
        if (remainingPendingUpdates.isEmpty()) {
            callback.onSuccess();
        } else {
            Log.d(TAG, "Found " + remainingPendingUpdates.size() + " desks still pending_update, triggering another sync...");
            syncDesks(callback);
        }
    }


    private void createDeskOnServer(final Desk localDesk, final SyncCallback callback) {
        Log.d(TAG, "Creating desk on server: " + localDesk.getName());
        DeskDto deskDto = new DeskDto();
        deskDto.setName(localDesk.getName());
        String folderServerId = idMappingRepository.getServerIdByLocalId(localDesk.getFolderId(), "folder");
        deskDto.setFolderId(folderServerId);
        deskDto.setPublic(localDesk.isPublic());

        Date createdAt, lastModified;
        try {
            createdAt = localDateFormat.parse(localDesk.getCreatedAt());
            lastModified = localDateFormat.parse(localDesk.getLastModified());
        } catch (ParseException e) {
            // Fallback to parsing "yyyy-MM-dd" format
            SimpleDateFormat fallbackFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            fallbackFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                createdAt = fallbackFormat.parse(localDesk.getCreatedAt());
                lastModified = fallbackFormat.parse(localDesk.getLastModified());
                // Update local desk to include time component for consistency
                localDesk.setCreatedAt(localDateFormat.format(createdAt));
                localDesk.setLastModified(localDateFormat.format(lastModified));
                deskRepository.updateDesk(localDesk);
                Log.d(TAG, "Updated desk " + localDesk.getName() + " with full date format");
            } catch (ParseException ex) {
                Log.e(TAG, "Error parsing date for desk " + localDesk.getName() + ": " + ex.getMessage());
                callback.onFailure("Error parsing date: " + ex.getMessage());
                return;
            }
        }
        deskDto.setCreatedAt(createdAt);
        deskDto.setLastModified(lastModified);

        apiService.createDesk(deskDto).enqueue(new Callback<DeskDto>() {
            @Override
            public void onResponse(Call<DeskDto> call, Response<DeskDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeskDto serverDesk = response.body();
                    localDesk.setServerId(serverDesk.getId());
                    localDesk.setLastModified(dateFormat.format(serverDesk.getLastModified()));
                    // Nếu localDesk có folderId nhưng server không có, đánh dấu pending_update
                    if (localDesk.getFolderId() != null && folderServerId == null) {
                        deskRepository.updateSyncStatus(localDesk.getId(), "pending_update");
                        Log.d(TAG, "Desk " + localDesk.getName() + " marked as pending_update due to missing folder serverId");
                    } else {
                        deskRepository.updateSyncStatus(localDesk.getId(), "synced");
                    }
                    deskRepository.insertIdMapping(localDesk.getId(), serverDesk.getId());
                    Log.d(TAG, "Created desk on server with serverId: " + serverDesk.getId());
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to create desk: " + response.code() + " - " + response.message());
                    callback.onFailure("Failed to create desk: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<DeskDto> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void updateDeskOnServer(final Desk localDesk, final SyncCallback callback) {
        Log.d(TAG, "Updating desk on server: " + localDesk.getName());
        String serverId = idMappingRepository.getServerIdByLocalId(localDesk.getId(), "desk");
        if (serverId == null) {
            Log.d(TAG, "No serverId for desk " + localDesk.getId() + ", treating as new desk");
            createDeskOnServer(localDesk, callback);
            return;
        }

        DeskDto deskDto = new DeskDto();
        deskDto.setName(localDesk.getName());
        deskDto.setFolderId(idMappingRepository.getServerIdByLocalId(localDesk.getFolderId(), "folder") != null ?
                idMappingRepository.getServerIdByLocalId(localDesk.getFolderId(), "folder") : null);
        deskDto.setPublic(localDesk.isPublic());

        Date createdAt, lastModified;
        try {
            createdAt = localDateFormat.parse(localDesk.getCreatedAt());
            lastModified = localDateFormat.parse(localDesk.getLastModified());
        } catch (ParseException e) {
            // Fallback to parsing "yyyy-MM-dd" format
            SimpleDateFormat fallbackFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            fallbackFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                createdAt = fallbackFormat.parse(localDesk.getCreatedAt());
                lastModified = fallbackFormat.parse(localDesk.getLastModified());
                // Update local desk to include time component for consistency
                localDesk.setCreatedAt(localDateFormat.format(createdAt));
                localDesk.setLastModified(localDateFormat.format(lastModified));
                deskRepository.updateDesk(localDesk);
                Log.d(TAG, "Updated desk " + localDesk.getName() + " with full date format");
            } catch (ParseException ex) {
                Log.e(TAG, "Error parsing date for desk " + localDesk.getName() + ": " + ex.getMessage());
                callback.onFailure("Error parsing date: " + ex.getMessage());
                return;
            }
        }
        deskDto.setCreatedAt(createdAt);
        deskDto.setLastModified(lastModified);

        apiService.updateDesk(serverId, deskDto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    deskRepository.updateSyncStatus(localDesk.getId(), "synced");
                    Log.d(TAG, "Updated desk on server with serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to update desk: " + response.code() + " - " + response.message());
                    callback.onFailure("Failed to update desk: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void deleteDeskOnServer(final Desk localDesk, final SyncCallback callback) {
        Log.d(TAG, "Deleting desk on server: " + localDesk.getName());
        String serverId = idMappingRepository.getServerIdByLocalId(localDesk.getId(), "desk");
        if (serverId == null) {
            deskRepository.deleteDeskConfirmed(localDesk.getId());
            Log.d(TAG, "Deleted local desk with no serverId: " + localDesk.getId());
            callback.onSuccess();
            return;
        }

        apiService.deleteDesk(serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    deskRepository.deleteDeskConfirmed(localDesk.getId());
                    idMappingRepository.deleteIdMapping(localDesk.getId(), "desk");
                    Log.d(TAG, "Deleted desk on server with serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to delete desk: " + response.code() + " - " + response.message());
                    callback.onFailure("Failed to delete desk: " + response.code() + " - " + response.message());
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
            Log.d("FolderData", "Folder: " + folder.getName() + ", CreatedAt: " + folder.getCreatedAt() + ", LastModified: " + folder.getLastModified());
        }
        for (Folder folder : allFolders) {
            if (folder.getId() == localId) {
                return folder.getServerId();
            }
        }
        return null;
    }

    public interface SyncCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
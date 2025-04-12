package com.cntt2.flashcard.sync;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.remote.ApiClient;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.CardDto;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.remote.dto.GetFolderDto;
import com.cntt2.flashcard.data.remote.dto.PostFolderDto;
import com.cntt2.flashcard.data.remote.dto.ReviewDto;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.data.repository.FolderRepository;
import com.cntt2.flashcard.data.repository.IdMappingRepository;
import com.cntt2.flashcard.data.repository.ReviewRepository;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.Review;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private final CardRepository cardRepository;
    private final ReviewRepository reviewRepository;
    private final IdMappingRepository idMappingRepository;
    private final ApiService apiService;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat minuteFormat;

    public SyncManager(Context context) {
        this.folderRepository = App.getInstance().getFolderRepository();
        this.deskRepository = App.getInstance().getDeskRepository();
        this.cardRepository = App.getInstance().getCardRepository();
        this.reviewRepository = App.getInstance().getReviewRepository();
        this.idMappingRepository = new IdMappingRepository(context);
        this.apiService = ApiClient.getApiService();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.minuteFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
        this.minuteFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private Date truncateToMinute(Date date) {
        try {
            String minuteStr = minuteFormat.format(date);
            return minuteFormat.parse(minuteStr);
        } catch (ParseException e) {
            Log.e(TAG, "Error truncating date to minute: " + e.getMessage());
            return date;
        }
    }

    // FOLDER SYNC SECTION
    public void syncFolders(final SyncCallback callback) {
        Log.d(TAG, "Starting folder sync...");
        pullFoldersFromServer(new SyncCallback() {
            @Override
            public void onSuccess() {
                pushFoldersToServer(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        List<Folder> pendingFolders = new ArrayList<>();
                        pendingFolders.addAll(folderRepository.getPendingFolders("pending_create"));
                        pendingFolders.addAll(folderRepository.getPendingFolders("pending_update"));
                        pendingFolders.addAll(folderRepository.getPendingFolders("pending_delete"));
                        if (!pendingFolders.isEmpty()) {
                            Log.d(TAG, "Pending folders remain: " + pendingFolders.size());
                            syncFolders(callback);
                        } else {
                            Log.d(TAG, "Folder sync completed successfully");
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
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
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Failed to pull folders: " + response.code() + " - " + response.message());
                    callback.onFailure("Failed to pull folders: " + response.message());
                    return;
                }

                List<GetFolderDto> serverFolders = response.body();
                Log.d(TAG, "Successfully pulled " + serverFolders.size() + " folders from server");

                for (GetFolderDto serverFolder : serverFolders) {
                    if (serverFolder.getId() == null || serverFolder.getLastModified() == null) {
                        Log.e(TAG, "Invalid server folder data, skipping: " + serverFolder.getName());
                        continue;
                    }

                    Integer localId = idMappingRepository.getLocalIdByServerId(serverFolder.getId(), "folder");
                    Folder localFolder = localId != null ? folderRepository.getFolderById(localId) : null;

                    try {
                        Date serverLastModified = truncateToMinute(serverFolder.getLastModified());
                        String formattedLastModified = dateFormat.format(serverFolder.getLastModified());

                        if (localFolder == null) {
                            Folder newFolder = new Folder();
                            newFolder.setServerId(serverFolder.getId());
                            newFolder.setName(serverFolder.getName());
                            if (serverFolder.getParentFolderId() != null) {
                                newFolder.setParentFolderId(idMappingRepository.getLocalIdByServerId(serverFolder.getParentFolderId(), "folder"));
                            }
                            newFolder.setCreatedAt(dateFormat.format(serverFolder.getCreatedAt()));
                            newFolder.setLastModified(formattedLastModified);
                            newFolder.setSyncStatus("synced");
                            long newLocalId = folderRepository.insertFolder(newFolder);
                            idMappingRepository.insertIdMapping(new IdMapping((int) newLocalId, serverFolder.getId(), "folder"));
                            Log.d(TAG, "Inserted new folder with serverId: " + serverFolder.getId());
                        } else {
                            Date localLastModified = truncateToMinute(dateFormat.parse(localFolder.getLastModified()));
                            if ("pending_delete".equals(localFolder.getSyncStatus())) {
                                Log.d(TAG, "Skipping update for folder marked as pending_delete: " + localFolder.getId());
                                continue;
                            }
                            if (serverLastModified.after(localLastModified) && !"pending_update".equals(localFolder.getSyncStatus())) {
                                localFolder.setName(serverFolder.getName());
                                if (serverFolder.getParentFolderId() != null) {
                                    localFolder.setParentFolderId(idMappingRepository.getLocalIdByServerId(serverFolder.getParentFolderId(), "folder"));
                                }
                                localFolder.setLastModified(formattedLastModified);
                                folderRepository.updateFolder(localFolder, true);
                                folderRepository.updateSyncStatus(localFolder.getId(), "synced");
                                Log.d(TAG, "Updated folder with localId: " + localFolder.getId());
                            } else if (localLastModified.after(serverLastModified) && !"pending_update".equals(localFolder.getSyncStatus())) {
                                folderRepository.updateSyncStatus(localFolder.getId(), "pending_update");
                                Log.d(TAG, "Local folder is newer, marked as pending_update: " + localFolder.getId());
                            }
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing date for folder " + serverFolder.getName() + ": " + e.getMessage());
                    }
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(Call<List<GetFolderDto>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void pushFoldersToServer(final SyncCallback callback) {
        Log.d(TAG, "Pushing folders to server...");
        List<Folder> pendingFolders = new ArrayList<>();
        pendingFolders.addAll(folderRepository.getPendingFolders("pending_create"));
        pendingFolders.addAll(folderRepository.getPendingFolders("pending_update"));
        pendingFolders.addAll(folderRepository.getPendingFolders("pending_delete"));

        if (pendingFolders.isEmpty()) {
            callback.onSuccess();
            return;
        }

        final int totalPending = pendingFolders.size();
        final int[] completedTasks = {0};

        for (Folder folder : pendingFolders) {
            switch (folder.getSyncStatus()) {
                case "pending_create":
                    createFolderOnServer(folder, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) callback.onSuccess();
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure(error);
                        }
                    });
                    break;
                case "pending_update":
                    updateFolderOnServer(folder, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) callback.onSuccess();
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure(error);
                        }
                    });
                    break;
                case "pending_delete":
                    deleteFolderOnServer(folder, pendingFolders, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) callback.onSuccess();
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure(error);
                        }
                    });
                    break;
            }
        }
    }

    private void createFolderOnServer(final Folder folder, final SyncCallback callback) {
        String parentServerId = folder.getParentFolderId() != null ?
                idMappingRepository.getServerIdByLocalId(folder.getParentFolderId(), "folder") : null;
        boolean parentNotSynced = folder.getParentFolderId() != null && parentServerId == null;

        PostFolderDto dto = new PostFolderDto();
        dto.setName(folder.getName());
        dto.setParentFolderId(parentServerId);

        try {
            Date createdAt = dateFormat.parse(folder.getCreatedAt());
            Date lastModified = dateFormat.parse(folder.getLastModified());
            dto.setCreatedAt(createdAt);
            dto.setLastModified(lastModified);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for folder " + folder.getName() + ": " + e.getMessage());
            callback.onFailure("Date parsing error");
            return;
        }

        apiService.createFolder(dto).enqueue(new Callback<GetFolderDto>() {
            @Override
            public void onResponse(Call<GetFolderDto> call, Response<GetFolderDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GetFolderDto serverFolder = response.body();
                    folder.setServerId(serverFolder.getId());
                    folder.setLastModified(dateFormat.format(serverFolder.getLastModified()));
                    folderRepository.updateFolder(folder, true);
                    idMappingRepository.insertIdMapping(new IdMapping(folder.getId(), serverFolder.getId(), "folder"));
                    if (parentNotSynced) {
                        folderRepository.updateSyncStatus(folder.getId(), "pending_update");
                        Log.d(TAG, "Created folder on server with serverId: " + serverFolder.getId() + ", syncStatus: pending_update due to unsynced parent");
                    } else {
                        folderRepository.updateSyncStatus(folder.getId(), "synced");
                        Log.d(TAG, "Created folder on server with serverId: " + serverFolder.getId() + ", syncStatus: synced");
                    }
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to create folder: " + response.code());
                    callback.onFailure("Failed to create folder: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<GetFolderDto> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void updateFolderOnServer(final Folder folder, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(folder.getId(), "folder");
        if (serverId == null) {
            createFolderOnServer(folder, callback);
            return;
        }

        String parentServerId = folder.getParentFolderId() != null ?
                idMappingRepository.getServerIdByLocalId(folder.getParentFolderId(), "folder") : null;
        boolean parentNotSynced = folder.getParentFolderId() != null && parentServerId == null;

        PostFolderDto dto = new PostFolderDto();
        dto.setName(folder.getName());
        dto.setParentFolderId(parentServerId);

        try {
            Date createdAt = dateFormat.parse(folder.getCreatedAt());
            Date lastModified = dateFormat.parse(folder.getLastModified());
            dto.setCreatedAt(createdAt);
            dto.setLastModified(lastModified);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for folder " + folder.getName() + ": " + e.getMessage());
            callback.onFailure("Date parsing error");
            return;
        }

        apiService.updateFolder(serverId, dto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    folder.setLastModified(dateFormat.format(new Date()));
                    folderRepository.updateFolder(folder, true);
                    if (parentNotSynced) {
                        folderRepository.updateSyncStatus(folder.getId(), "pending_update");
                        Log.d(TAG, "Updated folder on server with serverId: " + serverId + ", syncStatus: pending_update due to unsynced parent");
                    } else {
                        folderRepository.updateSyncStatus(folder.getId(), "synced");
                        Log.d(TAG, "Updated folder on server with serverId: " + serverId + ", syncStatus: synced");
                    }
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to update folder: " + response.code());
                    callback.onFailure("Failed to update folder: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void deleteFolderOnServer(final Folder folder, List<Folder> pendingFolders, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(folder.getId(), "folder");
        if (serverId == null) {
            folderRepository.deleteFolderConfirmed(folder.getId());
            Log.d(TAG, "No serverId found, deleted folder locally: " + folder.getId());
            pendingFolders.remove(folder);
            callback.onSuccess();
            return;
        }

        apiService.deleteFolder(serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    folderRepository.deleteFolderConfirmed(folder.getId());
                    pendingFolders.remove(folder);
                    Log.d(TAG, "Deleted folder on server with serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to delete folder: " + response.code());
                    callback.onFailure("Failed to delete folder: " + response.message());
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
                pushDesksToServer(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        List<Desk> pendingDesks = new ArrayList<>();
                        pendingDesks.addAll(deskRepository.getPendingDesks("pending_create"));
                        pendingDesks.addAll(deskRepository.getPendingDesks("pending_update"));
                        pendingDesks.addAll(deskRepository.getPendingDesks("pending_delete"));
                        if (!pendingDesks.isEmpty()) {
                            Log.d(TAG, "Pending desks remain: " + pendingDesks.size());
                            for (Desk desk : pendingDesks) {
                                Log.d(TAG, "Pending desk - ID: " + desk.getId() + ", ServerId: " + desk.getServerId() + ", SyncStatus: " + desk.getSyncStatus());
                            }
                            syncDesks(callback);
                        } else {
                            Log.d(TAG, "Desk sync completed successfully");
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
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
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Failed to pull desks: " + response.code() + " - " + response.message());
                    callback.onFailure("Failed to pull desks: " + response.message());
                    return;
                }

                List<DeskDto> serverDesks = response.body();
                Log.d(TAG, "Successfully pulled " + serverDesks.size() + " desks from server");

                for (DeskDto serverDesk : serverDesks) {
                    if (serverDesk.getId() == null || serverDesk.getLastModified() == null) {
                        Log.e(TAG, "Invalid server desk data, skipping: " + serverDesk.getName());
                        continue;
                    }

                    Integer localId = idMappingRepository.getLocalIdByServerId(serverDesk.getId(), "desk");
                    Desk localDesk = localId != null ? deskRepository.getDeskById(localId) : null;

                    try {
                        Date serverLastModified = truncateToMinute(serverDesk.getLastModified());
                        String formattedLastModified = dateFormat.format(serverDesk.getLastModified());

                        if (localDesk == null) {
                            Desk newDesk = new Desk();
                            newDesk.setServerId(serverDesk.getId());
                            newDesk.setName(serverDesk.getName());
                            if (serverDesk.getFolderId() != null) {
                                newDesk.setFolderId(idMappingRepository.getLocalIdByServerId(serverDesk.getFolderId(), "folder"));
                            }
                            newDesk.setPublic(serverDesk.isPublic());
                            newDesk.setCreatedAt(dateFormat.format(serverDesk.getCreatedAt()));
                            newDesk.setLastModified(formattedLastModified);
                            newDesk.setSyncStatus("synced");
                            long newLocalId = deskRepository.insertDesk(newDesk);
                            idMappingRepository.insertIdMapping(new IdMapping((int) newLocalId, serverDesk.getId(), "desk"));
                            Log.d(TAG, "Inserted new desk with serverId: " + serverDesk.getId());
                        } else {
                            Date localLastModified = truncateToMinute(dateFormat.parse(localDesk.getLastModified()));
                            if ("pending_delete".equals(localDesk.getSyncStatus())) {
                                Log.d(TAG, "Skipping update for desk marked as pending_delete: " + localDesk.getId());
                                continue;
                            }
                            if (serverLastModified.after(localLastModified) && !"pending_update".equals(localDesk.getSyncStatus())) {
                                localDesk.setName(serverDesk.getName());
                                if (serverDesk.getFolderId() != null) {
                                    localDesk.setFolderId(idMappingRepository.getLocalIdByServerId(serverDesk.getFolderId(), "folder"));
                                }
                                localDesk.setPublic(serverDesk.isPublic());
                                localDesk.setLastModified(formattedLastModified);
                                deskRepository.updateDesk(localDesk, true);
                                deskRepository.updateSyncStatus(localDesk.getId(), "synced");
                                Log.d(TAG, "Updated desk with localId: " + localDesk.getId());
                            } else if (localLastModified.after(serverLastModified) && !"pending_update".equals(localDesk.getSyncStatus())) {
                                deskRepository.updateSyncStatus(localDesk.getId(), "pending_update");
                                Log.d(TAG, "Local desk is newer, marked as pending_update: " + localDesk.getId());
                            }
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing date for desk " + serverDesk.getName() + ": " + e.getMessage());
                    }
                }
                callback.onSuccess();
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
        List<Desk> pendingDesks = new ArrayList<>();
        pendingDesks.addAll(deskRepository.getPendingDesks("pending_create"));
        pendingDesks.addAll(deskRepository.getPendingDesks("pending_update"));
        pendingDesks.addAll(deskRepository.getPendingDesks("pending_delete"));

        if (pendingDesks.isEmpty()) {
            Log.d(TAG, "No pending desks to sync");
            callback.onSuccess();
            return;
        }

        Log.d(TAG, "Processing " + pendingDesks.size() + " pending desks");
        for (Desk desk : pendingDesks) {
            Log.d(TAG, "Processing desk - ID: " + desk.getId() + ", ServerId: " + desk.getServerId() + ", SyncStatus: " + desk.getSyncStatus());
        }

        final int totalPending = pendingDesks.size();
        final int[] completedTasks = {0};

        for (Desk desk : pendingDesks) {
            switch (desk.getSyncStatus()) {
                case "pending_create":
                    createDeskOnServer(desk, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) {
                                Log.d(TAG, "Completed all desk sync tasks");
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure(error);
                        }
                    });
                    break;
                case "pending_update":
                    updateDeskOnServer(desk, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) {
                                Log.d(TAG, "Completed all desk sync tasks");
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure(error);
                        }
                    });
                    break;
                case "pending_delete":
                    deleteDeskOnServer(desk, pendingDesks, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) {
                                Log.d(TAG, "Completed all desk sync tasks");
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure(error);
                        }
                    });
                    break;
            }
        }
    }

    private void createDeskOnServer(final Desk desk, final SyncCallback callback) {
        String folderServerId = desk.getFolderId() != null ?
                idMappingRepository.getServerIdByLocalId(desk.getFolderId(), "folder") : null;
        boolean folderNotSynced = desk.getFolderId() != null && folderServerId == null;

        DeskDto dto = new DeskDto();
        dto.setName(desk.getName());
        dto.setFolderId(folderServerId);
        dto.setPublic(desk.isPublic());

        try {
            Date createdAt = dateFormat.parse(desk.getCreatedAt());
            Date lastModified = dateFormat.parse(desk.getLastModified());
            dto.setCreatedAt(createdAt);
            dto.setLastModified(lastModified);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for desk " + desk.getName() + ": " + e.getMessage());
            callback.onFailure("Date parsing error");
            return;
        }

        apiService.createDesk(dto).enqueue(new Callback<DeskDto>() {
            @Override
            public void onResponse(Call<DeskDto> call, Response<DeskDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeskDto serverDesk = response.body();
                    desk.setServerId(serverDesk.getId());
                    desk.setLastModified(dateFormat.format(serverDesk.getLastModified()));
                    deskRepository.updateDesk(desk, true);
                    idMappingRepository.insertIdMapping(new IdMapping(desk.getId(), serverDesk.getId(), "desk"));
                    if (folderNotSynced) {
                        deskRepository.updateSyncStatus(desk.getId(), "pending_update");
                        Log.d(TAG, "Created desk on server with serverId: " + serverDesk.getId() + ", syncStatus: pending_update due to unsynced folder");
                    } else {
                        deskRepository.updateSyncStatus(desk.getId(), "synced");
                        Log.d(TAG, "Created desk on server with serverId: " + serverDesk.getId() + ", syncStatus: synced");
                    }
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to create desk: " + response.code());
                    callback.onFailure("Failed to create desk: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<DeskDto> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void updateDeskOnServer(final Desk desk, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(desk.getId(), "desk");
        if (serverId == null) {
            createDeskOnServer(desk, callback);
            return;
        }

        String folderServerId = desk.getFolderId() != null ?
                idMappingRepository.getServerIdByLocalId(desk.getFolderId(), "folder") : null;
        boolean folderNotSynced = desk.getFolderId() != null && folderServerId == null;

        DeskDto dto = new DeskDto();
        dto.setName(desk.getName());
        dto.setFolderId(folderServerId);
        dto.setPublic(desk.isPublic());

        try {
            Date createdAt = dateFormat.parse(desk.getCreatedAt());
            Date lastModified = dateFormat.parse(desk.getLastModified());
            dto.setCreatedAt(createdAt);
            dto.setLastModified(lastModified);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for desk " + desk.getName() + ": " + e.getMessage());
            callback.onFailure("Date parsing error");
            return;
        }

        apiService.updateDesk(serverId, dto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    desk.setLastModified(dateFormat.format(new Date()));
                    deskRepository.updateDesk(desk, true);
                    if (folderNotSynced) {
                        deskRepository.updateSyncStatus(desk.getId(), "pending_update");
                        Log.d(TAG, "Updated desk on server with serverId: " + serverId + ", syncStatus: pending_update due to unsynced folder");
                    } else {
                        deskRepository.updateSyncStatus(desk.getId(), "synced");
                        Log.d(TAG, "Updated desk on server with serverId: " + serverId + ", syncStatus: synced");
                    }
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to update desk: " + response.code());
                    callback.onFailure("Failed to update desk: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void deleteDeskOnServer(final Desk desk, final List<Desk> pendingDesks, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(desk.getId(), "desk");
        if (serverId == null) {
            deskRepository.deleteDeskConfirmed(desk.getId());
            Log.d(TAG, "No serverId found, deleted desk locally: " + desk.getId());
            pendingDesks.remove(desk);
            callback.onSuccess();
            return;
        }

        apiService.deleteDesk(serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    deskRepository.deleteDeskConfirmed(desk.getId());
                    pendingDesks.remove(desk);
                    Log.d(TAG, "Deleted desk on server with serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to delete desk: " + response.code());
                    callback.onFailure("Failed to delete desk: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }



    // SYNC CARD SECTION
    public void syncCards(final SyncCallback callback) {
        Log.d(TAG, "Starting card sync...");
        pullCardsFromServer(new SyncCallback() {
            @Override
            public void onSuccess() {
                pushCardsToServer(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        List<Card> pendingCards = new ArrayList<>();
                        pendingCards.addAll(cardRepository.getPendingCards("pending_create"));
                        pendingCards.addAll(cardRepository.getPendingCards("pending_update"));
                        pendingCards.addAll(cardRepository.getPendingCards("pending_delete"));
                        if (!pendingCards.isEmpty()) {
                            Log.d(TAG, "Pending cards remain: " + pendingCards.size());
                            for (Card card : pendingCards) {
                                Log.d(TAG, "Pending card - ID: " + card.getId() + ", ServerId: " + card.getServerId() + ", SyncStatus: " + card.getSyncStatus());
                            }
                            syncCards(callback);
                        } else {
                            Log.d(TAG, "Card sync completed successfully");
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    private void pullCardsFromServer(final SyncCallback callback) {
        Log.d(TAG, "Pulling cards from server...");
        List<Desk> desks = deskRepository.getAllDesks();
        if (desks.isEmpty()) {
            Log.d(TAG, "No desks found, skipping card sync");
            callback.onSuccess();
            return;
        }

        final int totalDesks = desks.size();
        final int[] completedDesks = {0};

        for (Desk desk : desks) {
            String serverDeskId = idMappingRepository.getServerIdByLocalId(desk.getId(), "desk");
            if (serverDeskId == null) {
                Log.d(TAG, "Desk not synced, skipping cards for deskId: " + desk.getId());
                completedDesks[0]++;
                if (completedDesks[0] == totalDesks) callback.onSuccess();
                continue;
            }

            apiService.getCardsByDeskId(serverDeskId).enqueue(new Callback<List<CardDto>>() {
                @Override
                public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "Failed to pull cards for deskId: " + serverDeskId + ", code: " + response.code());
                        callback.onFailure("Failed to pull cards: " + response.message());
                        return;
                    }

                    List<CardDto> serverCards = response.body();
                    Log.d(TAG, "Successfully pulled " + serverCards.size() + " cards for deskId: " + serverDeskId);

                    for (CardDto serverCard : serverCards) {
                        if (serverCard.getId() == null || serverCard.getLastModified() == null) {
                            Log.e(TAG, "Invalid server card data, skipping for deskId: " + serverDeskId);
                            continue;
                        }

                        Integer localId = idMappingRepository.getLocalIdByServerId(serverCard.getId(), "card");
                        Card localCard = localId != null ? cardRepository.getCardById(localId) : null;

                        try {
                            Date serverLastModified = truncateToMinute(serverCard.getLastModified());
                            String formattedLastModified = dateFormat.format(serverCard.getLastModified());

                            if (localCard == null) {
                                Card newCard = new Card();
                                newCard.setServerId(serverCard.getId());
                                newCard.setDeskId(desk.getId());
                                newCard.setFront(serverCard.getFront());
                                newCard.setBack(serverCard.getBack());
                                newCard.setCreatedAt(dateFormat.format(serverCard.getCreatedAt()));
                                newCard.setLastModified(formattedLastModified);
                                newCard.setSyncStatus("synced");
                                long newLocalId = cardRepository.insertCard(newCard);
                                idMappingRepository.insertIdMapping(new IdMapping((int) newLocalId, serverCard.getId(), "card"));
                                Log.d(TAG, "Inserted new card with serverId: " + serverCard.getId());
                            } else {
                                Date localLastModified = truncateToMinute(dateFormat.parse(localCard.getLastModified()));
                                if ("pending_delete".equals(localCard.getSyncStatus())) {
                                    Log.d(TAG, "Skipping update for card marked as pending_delete: " + localCard.getId());
                                    continue;
                                }
                                if (serverLastModified.after(localLastModified) && !"pending_update".equals(localCard.getSyncStatus())) {
                                    localCard.setFront(serverCard.getFront());
                                    localCard.setBack(serverCard.getBack());
                                    localCard.setDeskId(desk.getId());
                                    localCard.setLastModified(formattedLastModified);
                                    cardRepository.updateCard(localCard, true);
                                    cardRepository.updateSyncStatus(localCard.getId(), "synced");
                                    Log.d(TAG, "Updated card with localId: " + localCard.getId());
                                } else if (localLastModified.after(serverLastModified) && !"pending_update".equals(localCard.getSyncStatus())) {
                                    cardRepository.updateSyncStatus(localCard.getId(), "pending_update");
                                    Log.d(TAG, "Local card is newer, marked as pending_update: " + localCard.getId());
                                }
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing date for card in deskId: " + serverDeskId + ": " + e.getMessage());
                        }
                    }

                    completedDesks[0]++;
                    if (completedDesks[0] == totalDesks) callback.onSuccess();
                }

                @Override
                public void onFailure(Call<List<CardDto>> call, Throwable t) {
                    Log.e(TAG, "Network error pulling cards for deskId: " + serverDeskId + ": " + t.getMessage());
                    callback.onFailure("Network error: " + t.getMessage());
                }
            });
        }
    }

    private void pushCardsToServer(final SyncCallback callback) {
        Log.d(TAG, "Pushing cards to server...");
        List<Card> pendingCards = new ArrayList<>();
        pendingCards.addAll(cardRepository.getPendingCards("pending_create"));
        pendingCards.addAll(cardRepository.getPendingCards("pending_update"));
        pendingCards.addAll(cardRepository.getPendingCards("pending_delete"));

        if (pendingCards.isEmpty()) {
            Log.d(TAG, "No pending cards to sync");
            callback.onSuccess();
            return;
        }

        Log.d(TAG, "Processing " + pendingCards.size() + " pending cards");
        for (Card card : pendingCards) {
            Log.d(TAG, "Processing card - ID: " + card.getId() + ", ServerId: " + card.getServerId() + ", SyncStatus: " + card.getSyncStatus());
        }

        final int totalPending = pendingCards.size();
        final int[] completedTasks = {0};

        for (Card card : pendingCards) {
            switch (card.getSyncStatus()) {
                case "pending_create":
                    createCardOnServer(card, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) {
                                Log.d(TAG, "Completed all card sync tasks");
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure(error);
                        }
                    });
                    break;
                case "pending_update":
                    updateCardOnServer(card, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) {
                                Log.d(TAG, "Completed all card sync tasks");
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure(error);
                        }
                    });
                    break;
                case "pending_delete":
                    deleteCardOnServer(card, pendingCards, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) {
                                Log.d(TAG, "Completed all card sync tasks");
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure(error);
                        }
                    });
                    break;
            }
        }
    }

    private void createCardOnServer(final Card card, final SyncCallback callback) {
        String serverDeskId = idMappingRepository.getServerIdByLocalId(card.getDeskId(), "desk");
        boolean deskNotSynced = card.getDeskId() != 0 && serverDeskId == null;

        if (deskNotSynced) {
            Log.d(TAG, "Desk not synced for cardId: " + card.getId() + ", marking as pending_create");
            callback.onSuccess();
            return;
        }

        CardDto dto = new CardDto();
        dto.setDeskId(serverDeskId);
        dto.setFront(card.getFront());
        dto.setBack(card.getBack());
        dto.setImagePaths(new ArrayList<>());

        try {
            Date createdAt = dateFormat.parse(card.getCreatedAt());
            Date lastModified = dateFormat.parse(card.getLastModified());
            dto.setCreatedAt(createdAt);
            dto.setLastModified(lastModified);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for cardId: " + card.getId() + ": " + e.getMessage());
            callback.onFailure("Date parsing error");
            return;
        }

        apiService.createCard(dto).enqueue(new Callback<CardDto>() {
            @Override
            public void onResponse(Call<CardDto> call, Response<CardDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CardDto serverCard = response.body();
                    card.setServerId(serverCard.getId());
                    card.setLastModified(dateFormat.format(serverCard.getLastModified()));
                    cardRepository.updateCard(card, true);
                    idMappingRepository.insertIdMapping(new IdMapping(card.getId(), serverCard.getId(), "card"));
                    cardRepository.updateSyncStatus(card.getId(), "synced");
                    Log.d(TAG, "Created card on server with serverId: " + serverCard.getId() + ", syncStatus: synced");
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to create card: " + response.code());
                    callback.onFailure("Failed to create card: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<CardDto> call, Throwable t) {
                Log.e(TAG, "Network error creating card: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void updateCardOnServer(final Card card, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(card.getId(), "card");
        if (serverId == null) {
            createCardOnServer(card, callback);
            return;
        }

        String serverDeskId = idMappingRepository.getServerIdByLocalId(card.getDeskId(), "desk");
        boolean deskNotSynced = card.getDeskId() != 0 && serverDeskId == null;

        if (deskNotSynced) {
            Log.d(TAG, "Desk not synced for cardId: " + card.getId() + ", marking as pending_update");
            callback.onSuccess();
            return;
        }

        CardDto dto = new CardDto();
        dto.setDeskId(serverDeskId);
        dto.setFront(card.getFront());
        dto.setBack(card.getBack());
        dto.setImagePaths(new ArrayList<>()); // Để trống vì chưa xử lý ảnh

        try {
            Date createdAt = dateFormat.parse(card.getCreatedAt());
            Date lastModified = dateFormat.parse(card.getLastModified());
            dto.setCreatedAt(createdAt);
            dto.setLastModified(lastModified);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date for cardId: " + card.getId() + ": " + e.getMessage());
            callback.onFailure("Date parsing error");
            return;
        }

        apiService.updateCard(serverId, dto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    card.setLastModified(dateFormat.format(new Date()));
                    cardRepository.updateCard(card, true);
                    cardRepository.updateSyncStatus(card.getId(), "synced");
                    Log.d(TAG, "Updated card on server with serverId: " + serverId + ", syncStatus: synced");
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to update card: " + response.code());
                    callback.onFailure("Failed to update card: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error updating card: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void deleteCardOnServer(final Card card, final List<Card> pendingCards, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(card.getId(), "card");
        if (serverId == null) {
            cardRepository.deleteCardConfirmed(card.getId());
            Log.d(TAG, "No serverId found, deleted card locally: " + card.getId());
            pendingCards.remove(card);
            callback.onSuccess();
            return;
        }

        apiService.deleteCard(serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    cardRepository.deleteCardConfirmed(card.getId());
                    pendingCards.remove(card);
                    Log.d(TAG, "Deleted card on server with serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to delete card: " + response.code());
                    callback.onFailure("Failed to delete card: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error deleting card: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }


    // SYNC REVIEW SECTION
    // SYNC REVIEW SECTION
    public void syncReviews(final SyncCallback callback) {
        Log.d(TAG, "Starting review sync...");
        cleanupOrphanedReviews(); // Thêm bước dọn dẹp trước khi đồng bộ
        pullReviewsFromServer(new SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Pull reviews completed, starting push...");
                pushReviewsToServer(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        List<Review> pendingReviews = new ArrayList<>();
                        pendingReviews.addAll(reviewRepository.getPendingReviews("pending_create"));
                        pendingReviews.addAll(reviewRepository.getPendingReviews("pending_update"));
                        pendingReviews.addAll(reviewRepository.getPendingReviews("pending_delete"));
                        if (!pendingReviews.isEmpty()) {
                            Log.w(TAG, "Pending reviews remain: " + pendingReviews.size());
                            for (Review review : pendingReviews) {
                                Log.d(TAG, "Pending review - ID: " + review.getId() +
                                        ", CardId: " + review.getCardId() +
                                        ", SyncStatus: " + review.getSyncStatus());
                            }
                            syncReviews(callback);
                        } else {
                            Log.d(TAG, "Review sync completed successfully");
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Push reviews failed: " + error);
                        callback.onFailure(error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Pull reviews failed: " + error);
                callback.onFailure(error);
            }
        });
    }

    private void pullReviewsFromServer(final SyncCallback callback) {
        Log.d(TAG, "Pulling reviews from server...");
        List<Card> cards = cardRepository.getAllCards();
        if (cards.isEmpty()) {
            Log.d(TAG, "No cards found, skipping review sync");
            callback.onSuccess();
            return;
        }

        final int totalCards = cards.size();
        final int[] completedCards = {0};

        Log.d(TAG, "Processing " + totalCards + " cards for review sync");

        for (Card card : cards) {
            String serverCardId = idMappingRepository.getServerIdByLocalId(card.getId(), "card");
            if (serverCardId == null) {
                Log.w(TAG, "Card not synced, skipping reviews for localCardId: " + card.getId());
                completedCards[0]++;
                if (completedCards[0] == totalCards) {
                    Log.d(TAG, "Completed pulling reviews for all cards");
                    callback.onSuccess();
                }
                continue;
            }

            Log.d(TAG, "Fetching review for serverCardId: " + serverCardId);
            apiService.getReviewByCardId(serverCardId).enqueue(new Callback<ReviewDto>() {
                @Override
                public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                    if (!response.isSuccessful()) {
                        if (response.code() == 404) {
                            Log.d(TAG, "No review found for serverCardId: " + serverCardId);
                        } else {
                            Log.e(TAG, "Failed to pull review for serverCardId: " + serverCardId +
                                    ", code: " + response.code());
                        }
                        completedCards[0]++;
                        if (completedCards[0] == totalCards) {
                            Log.d(TAG, "Completed pulling reviews for all cards");
                            callback.onSuccess();
                        }
                        return;
                    }

                    ReviewDto serverReview = response.body();
                    if (serverReview == null || serverReview.getId() == null) {
                        Log.w(TAG, "Invalid review data for serverCardId: " + serverCardId);
                        completedCards[0]++;
                        if (completedCards[0] == totalCards) {
                            Log.d(TAG, "Completed pulling reviews for all cards");
                            callback.onSuccess();
                        }
                        return;
                    }

                    Log.d(TAG, "Successfully pulled review for serverCardId: " + serverCardId +
                            ", serverReviewId: " + serverReview.getId() +
                            ", lastReviewed: " + serverReview.getLastReviewed());

                    Integer localReviewId = idMappingRepository.getLocalIdByServerId(
                            serverReview.getId(), "review");
                    Review localReview = localReviewId != null ?
                            reviewRepository.getReviewById(localReviewId) : null;

                    String currentTime = dateFormat.format(new Date());
                    if (localReview == null) {
                        // Chèn Review mới
                        Review newReview = new Review();
                        newReview.setServerId(serverReview.getId());
                        newReview.setCardId(card.getId());
                        newReview.setEase(serverReview.getEase());
                        newReview.setInterval(serverReview.getInterval());
                        newReview.setRepetition(serverReview.getRepetition());
                        newReview.setNextReviewDate(serverReview.getNextReviewDate());
                        newReview.setLastReviewed(serverReview.getLastReviewed());
                        newReview.setLastModified(currentTime);
                        newReview.setSyncStatus("synced");
                        long newLocalId = reviewRepository.insertReview(newReview);
                        idMappingRepository.insertIdMapping(
                                new IdMapping((int) newLocalId, serverReview.getId(), "review"));
                        Log.d(TAG, "Inserted new review with localId: " + newLocalId +
                                ", serverId: " + serverReview.getId());
                    } else {
                        // So sánh dựa trên lastReviewed
                        if ("pending_delete".equals(localReview.getSyncStatus()) ||
                                "pending_update".equals(localReview.getSyncStatus())) {
                            Log.d(TAG, "Skipping update for review with localId: " +
                                    localReview.getId() + ", status: " + localReview.getSyncStatus());
                        } else {
                            try {
                                boolean shouldUpdateLocal = false;
                                String serverLastReviewed = serverReview.getLastReviewed();
                                String localLastReviewed = localReview.getLastReviewed();

                                if (serverLastReviewed == null && localLastReviewed == null) {
                                    // Cả hai đều null, cập nhật local với dữ liệu server
                                    shouldUpdateLocal = true;
                                } else if (serverLastReviewed == null) {
                                    // Server null, local không null -> ưu tiên local
                                    reviewRepository.updateSyncStatus(localReview.getId(), "pending_update");
                                    Log.d(TAG, "Server lastReviewed is null, marked review as pending_update: " +
                                            localReview.getId());
                                } else if (localLastReviewed == null) {
                                    // Local null, server không null -> ưu tiên server
                                    shouldUpdateLocal = true;
                                } else {
                                    // Cả hai đều không null, so sánh ngày
                                    Date serverDate = dateFormat.parse(serverLastReviewed);
                                    Date localDate = dateFormat.parse(localLastReviewed);
                                    if (serverDate.after(localDate)) {
                                        shouldUpdateLocal = true;
                                    } else if (localDate.after(serverDate)) {
                                        reviewRepository.updateSyncStatus(localReview.getId(), "pending_update");
                                        Log.d(TAG, "Local lastReviewed is newer, marked as pending_update: " +
                                                localReview.getId());
                                    }
                                }

                                if (shouldUpdateLocal) {
                                    localReview.setEase(serverReview.getEase());
                                    localReview.setInterval(serverReview.getInterval());
                                    localReview.setRepetition(serverReview.getRepetition());
                                    localReview.setNextReviewDate(serverReview.getNextReviewDate());
                                    localReview.setLastReviewed(serverReview.getLastReviewed());
                                    localReview.setLastModified(currentTime);
                                    reviewRepository.updateReview(localReview, true);
                                    reviewRepository.updateSyncStatus(localReview.getId(), "synced");
                                    Log.d(TAG, "Updated review with localId: " + localReview.getId() +
                                            ", serverId: " + serverReview.getId() +
                                            ", lastReviewed: " + serverReview.getLastReviewed());
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing lastReviewed for reviewId: " + localReview.getId() +
                                        ": " + e.getMessage());
                                // Nếu lỗi parse, ưu tiên server để đảm bảo đồng bộ
                                localReview.setEase(serverReview.getEase());
                                localReview.setInterval(serverReview.getInterval());
                                localReview.setRepetition(serverReview.getRepetition());
                                localReview.setNextReviewDate(serverReview.getNextReviewDate());
                                localReview.setLastReviewed(serverReview.getLastReviewed());
                                localReview.setLastModified(currentTime);
                                reviewRepository.updateReview(localReview, true);
                                reviewRepository.updateSyncStatus(localReview.getId(), "synced");
                                Log.d(TAG, "Updated review due to parse error with localId: " + localReview.getId());
                            }
                        }
                    }

                    completedCards[0]++;
                    if (completedCards[0] == totalCards) {
                        Log.d(TAG, "Completed pulling reviews for all cards");
                        callback.onSuccess();
                    }
                }

                @Override
                public void onFailure(Call<ReviewDto> call, Throwable t) {
                    Log.e(TAG, "Network error pulling review for serverCardId: " + serverCardId +
                            ": " + t.getMessage());
                    completedCards[0]++;
                    if (completedCards[0] == totalCards) {
                        Log.d(TAG, "Completed pulling reviews for all cards despite errors");
                        callback.onSuccess();
                    }
                }
            });
        }
    }

    private void pushReviewsToServer(final SyncCallback callback) {
        Log.d(TAG, "Pushing reviews to server...");
        List<Review> pendingReviews = new ArrayList<>();
        pendingReviews.addAll(reviewRepository.getPendingReviews("pending_create"));
        pendingReviews.addAll(reviewRepository.getPendingReviews("pending_update"));
        pendingReviews.addAll(reviewRepository.getPendingReviews("pending_delete"));

        if (pendingReviews.isEmpty()) {
            Log.d(TAG, "No pending reviews to sync");
            callback.onSuccess();
            return;
        }

        Log.d(TAG, "Processing " + pendingReviews.size() + " pending reviews");
        for (Review review : pendingReviews) {
            Log.d(TAG, "Pending review - ID: " + review.getId() +
                    ", CardId: " + review.getCardId() +
                    ", SyncStatus: " + review.getSyncStatus());
        }

        final int totalPending = pendingReviews.size();
        final int[] completedTasks = {0};

        for (Review review : pendingReviews) {
            // Kiểm tra Card liên quan
            Card card = cardRepository.getCardById(review.getCardId());
            if (card == null || "pending_delete".equals(card.getSyncStatus())) {
                Log.w(TAG, "Card not found or marked for deletion for reviewId: " + review.getId() +
                        ", marking review as pending_delete");
                review.setSyncStatus("pending_delete");
                reviewRepository.updateReview(review, false);
                reviewRepository.updateSyncStatus(review.getId(), "pending_delete");
                deleteReviewOnServer(review, pendingReviews, new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        completedTasks[0]++;
                        if (completedTasks[0] == totalPending) {
                            Log.d(TAG, "Completed all review sync tasks");
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to delete review ID: " + review.getId() + ": " + error);
                        callback.onFailure(error);
                    }
                });
                continue;
            }

            // Kiểm tra dữ liệu hợp lệ
            if (review.getCardId() == 0 || review.getEase() <= 0 || review.getNextReviewDate() == null) {
                Log.w(TAG, "Invalid review data for ID: " + review.getId() +
                        ", marking as pending_delete");
                review.setSyncStatus("pending_delete");
                reviewRepository.updateReview(review, false);
                reviewRepository.updateSyncStatus(review.getId(), "pending_delete");
                deleteReviewOnServer(review, pendingReviews, new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        completedTasks[0]++;
                        if (completedTasks[0] == totalPending) {
                            Log.d(TAG, "Completed all review sync tasks");
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to delete review ID: " + review.getId() + ": " + error);
                        callback.onFailure(error);
                    }
                });
                continue;
            }

            switch (review.getSyncStatus()) {
                case "pending_create":
                    createReviewOnServer(review, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) {
                                Log.d(TAG, "Completed all review sync tasks");
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "Failed to create review ID: " + review.getId() + ": " + error);
                            callback.onFailure(error);
                        }
                    });
                    break;
                case "pending_update":
                    updateReviewOnServer(review, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) {
                                Log.d(TAG, "Completed all review sync tasks");
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "Failed to update review ID: " + review.getId() + ": " + error);
                            callback.onFailure(error);
                        }
                    });
                    break;
                case "pending_delete":
                    deleteReviewOnServer(review, pendingReviews, new SyncCallback() {
                        @Override
                        public void onSuccess() {
                            completedTasks[0]++;
                            if (completedTasks[0] == totalPending) {
                                Log.d(TAG, "Completed all review sync tasks");
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "Failed to delete review ID: " + review.getId() + ": " + error);
                            callback.onFailure(error);
                        }
                    });
                    break;
            }
        }
    }

    private void createReviewOnServer(final Review review, final SyncCallback callback) {
        // Kiểm tra Card trước
        Card card = cardRepository.getCardById(review.getCardId());
        if (card == null || "pending_delete".equals(card.getSyncStatus())) {
            Log.w(TAG, "Card not found or marked for deletion for reviewId: " + review.getId() +
                    ", marking as pending_delete");
            review.setSyncStatus("pending_delete");
            reviewRepository.updateReview(review, false);
            reviewRepository.updateSyncStatus(review.getId(), "pending_delete");
            deleteReviewOnServer(review, new ArrayList<>(), callback);
            return;
        }

        String serverCardId = idMappingRepository.getServerIdByLocalId(review.getCardId(), "card");
        boolean cardNotSynced = review.getCardId() != 0 && serverCardId == null;

        if (cardNotSynced) {
            Log.w(TAG, "Card not synced for reviewId: " + review.getId() +
                    ", delaying sync until card is synced");
            callback.onSuccess();
            return;
        }

        ReviewDto dto = new ReviewDto();
        dto.setCardId(serverCardId);
        dto.setEase(review.getEase());
        dto.setInterval(review.getInterval());
        dto.setRepetition(review.getRepetition());
        dto.setNextReviewDate(review.getNextReviewDate());
        dto.setLastReviewed(review.getLastReviewed());

        // Log dữ liệu gửi đi để debug
        Log.d(TAG, "Creating review with DTO: " +
                "reviewId=" + review.getId() +
                ", cardId=" + dto.getCardId() +
                ", ease=" + dto.getEase() +
                ", interval=" + dto.getInterval() +
                ", repetition=" + dto.getRepetition() +
                ", nextReviewDate=" + dto.getNextReviewDate() +
                ", lastReviewed=" + dto.getLastReviewed());

        apiService.createReview(dto).enqueue(new Callback<ReviewDto>() {
            @Override
            public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReviewDto serverReview = response.body();
                    review.setServerId(serverReview.getId());
                    review.setLastModified(dateFormat.format(new Date()));
                    reviewRepository.updateReview(review, true);
                    idMappingRepository.insertIdMapping(
                            new IdMapping(review.getId(), serverReview.getId(), "review"));
                    reviewRepository.updateSyncStatus(review.getId(), "synced");
                    Log.d(TAG, "Created review on server with localId: " + review.getId() +
                            ", serverId: " + serverReview.getId() +
                            ", syncStatus: synced");
                    callback.onSuccess();
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading errorBody for reviewId: " + review.getId() +
                                ": " + e.getMessage());
                    }
                    Log.e(TAG, "Failed to create review for reviewId: " + review.getId() +
                            ", code: " + response.code() +
                            ", message: " + response.message() +
                            ", errorBody: " + errorBody);
                    callback.onFailure("Failed to create review for reviewId: " + review.getId() +
                            ": " + response.message() + ", error: " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<ReviewDto> call, Throwable t) {
                Log.e(TAG, "Network error creating review for reviewId: " + review.getId() +
                        ": " + t.getMessage());
                callback.onFailure("Network error for reviewId: " + review.getId() +
                        ": " + t.getMessage());
            }
        });
    }

    private void updateReviewOnServer(final Review review, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(review.getId(), "review");
        if (serverId == null) {
            Log.d(TAG, "No serverId found for reviewId: " + review.getId() +
                    ", treating as create");
            createReviewOnServer(review, callback);
            return;
        }

        // Kiểm tra Card trước
        Card card = cardRepository.getCardById(review.getCardId());
        if (card == null || "pending_delete".equals(card.getSyncStatus())) {
            Log.w(TAG, "Card not found or marked for deletion for reviewId: " + review.getId() +
                    ", marking as pending_delete");
            review.setSyncStatus("pending_delete");
            reviewRepository.updateReview(review, false);
            reviewRepository.updateSyncStatus(review.getId(), "pending_delete");
            deleteReviewOnServer(review, new ArrayList<>(), callback);
            return;
        }

        String serverCardId = idMappingRepository.getServerIdByLocalId(review.getCardId(), "card");
        boolean cardNotSynced = review.getCardId() != 0 && serverCardId == null;

        if (cardNotSynced) {
            Log.w(TAG, "Card not synced for reviewId: " + review.getId() +
                    ", delaying sync until card is synced");
            callback.onSuccess();
            return;
        }

        ReviewDto dto = new ReviewDto();
        dto.setCardId(serverCardId);
        dto.setEase(review.getEase());
        dto.setInterval(review.getInterval());
        dto.setRepetition(review.getRepetition());
        dto.setNextReviewDate(review.getNextReviewDate());
        dto.setLastReviewed(review.getLastReviewed());

        // Log dữ liệu gửi đi để debug
        Log.d(TAG, "Updating review with DTO: " +
                "reviewId=" + review.getId() +
                ", serverId=" + serverId +
                ", cardId=" + dto.getCardId() +
                ", ease=" + dto.getEase() +
                ", interval=" + dto.getInterval() +
                ", repetition=" + dto.getRepetition() +
                ", nextReviewDate=" + dto.getNextReviewDate() +
                ", lastReviewed=" + dto.getLastReviewed());

        apiService.updateReview(serverId, dto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    review.setLastModified(dateFormat.format(new Date()));
                    reviewRepository.updateReview(review, true);
                    reviewRepository.updateSyncStatus(review.getId(), "synced");
                    Log.d(TAG, "Updated review on server with localId: " + review.getId() +
                            ", serverId=" + serverId +
                            ", syncStatus: synced");
                    callback.onSuccess();
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading errorBody for reviewId: " + review.getId() +
                                ": " + e.getMessage());
                    }
                    Log.e(TAG, "Failed to update review for reviewId: " + review.getId() +
                            ", code: " + response.code() +
                            ", message: " + response.message() +
                            ", errorBody: " + errorBody);
                    callback.onFailure("Failed to update review for reviewId: " + review.getId() +
                            ": " + response.message() + ", error: " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error updating review for reviewId: " + review.getId() +
                        ": " + t.getMessage());
                callback.onFailure("Network error for reviewId: " + review.getId() +
                        ": " + t.getMessage());
            }
        });
    }

    private void deleteReviewOnServer(final Review review, final List<Review> pendingReviews,
                                      final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(review.getId(), "review");
        if (serverId == null) {
            reviewRepository.deleteReviewConfirmed(review.getId());
            Log.d(TAG, "No serverId found, deleted review locally with localId: " + review.getId());
            pendingReviews.remove(review);
            callback.onSuccess();
            return;
        }

        Log.d(TAG, "Deleting review with localId: " + review.getId() +
                ", serverId: " + serverId);

        apiService.deleteReview(serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() || response.code() == 404) { // Xử lý lỗi 404
                    reviewRepository.deleteReviewConfirmed(review.getId());
                    pendingReviews.remove(review);
                    Log.d(TAG, "Deleted review on server with localId: " + review.getId() +
                            ", serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading errorBody for reviewId: " + review.getId() +
                                ": " + e.getMessage());
                    }
                    Log.e(TAG, "Failed to delete review for reviewId: " + review.getId() +
                            ", code: " + response.code() +
                            ", message: " + response.message() +
                            ", errorBody: " + errorBody);
                    callback.onFailure("Failed to delete review for reviewId: " + review.getId() +
                            ": " + response.message() + ", error: " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error deleting review for reviewId: " + review.getId() +
                        ": " + t.getMessage());
                callback.onFailure("Network error for reviewId: " + review.getId() +
                        ": " + t.getMessage());
            }
        });
    }

    // Phương thức mới để dọn dẹp Review mồ côi
    public void cleanupOrphanedReviews() {
        Log.d(TAG, "Cleaning up orphaned reviews...");
        List<Review> allReviews = reviewRepository.getAllReviews();
        for (Review review : allReviews) {
            Card card = cardRepository.getCardById(review.getCardId());
            if (card == null || "pending_delete".equals(card.getSyncStatus())) {
                Log.w(TAG, "Found orphaned review - ID: " + review.getId() +
                        ", CardId: " + review.getCardId() +
                        ", marking as pending_delete");
                review.setSyncStatus("pending_delete");
                reviewRepository.updateReview(review, false);
                reviewRepository.updateSyncStatus(review.getId(), "pending_delete");
            }
        }
        Log.d(TAG, "Orphaned reviews cleanup completed");
    }

    public interface SyncCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
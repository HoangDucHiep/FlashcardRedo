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
import com.cntt2.flashcard.data.remote.dto.SessionDto;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.data.repository.FolderRepository;
import com.cntt2.flashcard.data.repository.IdMappingRepository;
import com.cntt2.flashcard.data.repository.LearningSessionRepository;
import com.cntt2.flashcard.data.repository.ReviewRepository;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.LearningSession;
import com.cntt2.flashcard.model.Review;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private final LearningSessionRepository sessionRepository;
    private final ApiService apiService;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat minuteFormat;

    public SyncManager(Context context) {
        this.folderRepository = App.getInstance().getFolderRepository();
        this.deskRepository = App.getInstance().getDeskRepository();
        this.cardRepository = App.getInstance().getCardRepository();
        this.reviewRepository = App.getInstance().getReviewRepository();
        this.sessionRepository = App.getInstance().getLearningSessionRepository();
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

        for (Folder folder : new ArrayList<>(pendingFolders)) {
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

        for (Desk desk : new ArrayList<>(pendingDesks)) {
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
                                Log.d(TAG, "Pending card - ID: " + card.getId() +
                                        ", DeskId: " + card.getDeskId() +
                                        ", SyncStatus: " + card.getSyncStatus());
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
                        Log.e(TAG, "Failed to pull cards for deskId: " + serverDeskId +
                                ", code: " + response.code());
                        completedDesks[0]++;
                        if (completedDesks[0] == totalDesks) callback.onSuccess();
                        return;
                    }

                    List<CardDto> serverCards = response.body();
                    Log.d(TAG, "Successfully pulled " + serverCards.size() + " cards for deskId: " + serverDeskId);

                    for (CardDto serverCard : serverCards) {
                        if (serverCard.getId() == null) {
                            Log.e(TAG, "Invalid server card data, skipping for deskId: " + serverDeskId);
                            continue;
                        }

                        String serverCardId = serverCard.getId();
                        Integer localId = idMappingRepository.getLocalIdByServerId(serverCardId, "card");
                        Card localCard = localId != null ? cardRepository.getCardById(localId) : null;

                        try {
                            String serverLastModifiedStr = String.valueOf(serverCard.getLastModified());
                            Date serverLastModified = parseFlexibleDate(serverLastModifiedStr);
                            String formattedLastModified = dateFormat.format(serverLastModified);

                            if (localCard == null) {
                                Card newCard = new Card();
                                newCard.setServerId(serverCardId);
                                newCard.setDeskId(desk.getId());
                                newCard.setFront(serverCard.getFront());
                                newCard.setBack(serverCard.getBack());
                                newCard.setCreatedAt(dateFormat.format(parseFlexibleDate(String.valueOf(serverCard.getCreatedAt()))));
                                newCard.setLastModified(formattedLastModified);
                                newCard.setSyncStatus("synced");
                                long newLocalId = cardRepository.insertCard(newCard, true);
                                idMappingRepository.insertIdMapping(new IdMapping((int) newLocalId, serverCardId, "card"));
                                Log.d(TAG, "Inserted new card with serverId: " + serverCardId);
                            } else {
                                Date localLastModified = dateFormat.parse(localCard.getLastModified());
                                serverLastModified = truncateToMinute(serverLastModified);
                                localLastModified = truncateToMinute(localLastModified);

                                if ("pending_delete".equals(localCard.getSyncStatus())) {
                                    Log.d(TAG, "Skipping update for card marked as pending_delete: " + localCard.getId());
                                    continue;
                                }
                                if (serverLastModified.after(localLastModified) && !"pending_update".equals(localCard.getSyncStatus())) {
                                    localCard.setFront(serverCard.getFront());
                                    localCard.setBack(serverCard.getBack());
                                    localCard.setLastModified(formattedLastModified);
                                    cardRepository.updateCard(localCard, true);
                                    cardRepository.updateSyncStatus(localCard.getId(), "synced");
                                    Log.d(TAG, "Updated card with localId: " + localCard.getId());
                                } else if (localLastModified.after(serverLastModified) && !"pending_update".equals(localCard.getSyncStatus())) {
                                    cardRepository.updateSyncStatus(localCard.getId(), "pending_update");
                                    Log.d(TAG, "Local card is newer, marked as pending_update: " + localCard.getId());
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing card for deskId: " + serverDeskId + ": " + e.getMessage());
                        }
                    }

                    completedDesks[0]++;
                    if (completedDesks[0] == totalDesks) callback.onSuccess();
                }

                @Override
                public void onFailure(Call<List<CardDto>> call, Throwable t) {
                    Log.e(TAG, "Network error pulling cards for deskId: " + serverDeskId + ": " + t.getMessage());
                    completedDesks[0]++;
                    if (completedDesks[0] == totalDesks) callback.onSuccess();
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

        // Xử lý các card pending_delete không có serverId trước
        Iterator<Card> iterator = pendingCards.iterator();
        while (iterator.hasNext()) {
            Card card = iterator.next();
            if ("pending_delete".equals(card.getSyncStatus()) &&
                    idMappingRepository.getServerIdByLocalId(card.getId(), "card") == null) {
                cardRepository.deleteCardConfirmed(card.getId());
                iterator.remove();
                Log.d(TAG, "Deleted card locally without serverId: " + card.getId());
            }
        }

        if (pendingCards.isEmpty()) {
            Log.d(TAG, "No pending cards to sync");
            callback.onSuccess();
            return;
        }

        final int totalPending = pendingCards.size();
        final int[] completedTasks = {0};

        for (Card card : new ArrayList<>(pendingCards)) {
            switch (card.getSyncStatus()) {
                case "pending_create":
                    createCardOnServer(card, new SyncCallback() {
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
                    updateCardOnServer(card, new SyncCallback() {
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
                    deleteCardOnServer(card, pendingCards, new SyncCallback() {
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
        String serverDeskId = idMappingRepository.getServerIdByLocalId(card.getDeskId(), "desk");
        if (serverId == null || serverDeskId == null) {
            cardRepository.deleteCardConfirmed(card.getId());
            pendingCards.remove(card);
            Log.d(TAG, "No serverId found, deleted card locally: " + card.getId());
            callback.onSuccess();
            return;
        }

        apiService.deleteCard(serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() || response.code() == 400) {
                    cardRepository.deleteCardConfirmed(card.getId());
                    pendingCards.remove(card);
                    Log.d(TAG, "Deleted card on server with serverId: " + serverId + " (HTTP " + response.code() + ")");
                    callback.onSuccess();
                } else {
                    cardRepository.deleteCardConfirmed(card.getId());
                    pendingCards.remove(card);
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

    // REVIEW SYNC SECTION
    public void syncReviews(final SyncCallback callback) {
        Log.d(TAG, "Starting review sync...");
        cleanOrphanedReviews();
        pushReviewsToServer(new SyncCallback() {
            @Override
            public void onSuccess() {
                pullReviewsFromServer(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        List<Review> pendingReviews = new ArrayList<>();
                        pendingReviews.addAll(reviewRepository.getPendingReviews("pending_create"));
                        pendingReviews.addAll(reviewRepository.getPendingReviews("pending_update"));
                        pendingReviews.addAll(reviewRepository.getPendingReviews("pending_delete"));
                        if (!pendingReviews.isEmpty()) {
                            Log.d(TAG, "Pending reviews remain: " + pendingReviews.size());
                            syncReviews(callback);
                        } else {
                            Log.d(TAG, "Review sync completed successfully");
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

    private void pullReviewsFromServer(final SyncCallback callback) {
        Log.d(TAG, "Pulling reviews from server...");
        cleanOrphanedReviews();

        List<Card> cards = cardRepository.getAllCards();
        Log.d(TAG, "Processing " + cards.size() + " cards for review sync");
        if (cards.isEmpty()) {
            Log.d(TAG, "No cards found, skipping review sync");
            callback.onSuccess();
            return;
        }

        final int totalCards = cards.size();
        final int[] completedCards = {0};

        for (Card card : cards) {
            String serverCardId = idMappingRepository.getServerIdByLocalId(card.getId(), "card");
            if (serverCardId == null) {
                Log.d(TAG, "Card not synced, skipping review for cardId: " + card.getId());
                completedCards[0]++;
                if (completedCards[0] == totalCards) callback.onSuccess();
                continue;
            }

            Log.d(TAG, "Fetching review for serverCardId: " + serverCardId);
            apiService.getReviewByCardId(serverCardId).enqueue(new Callback<ReviewDto>() {
                @Override
                public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "Failed to pull review for serverCardId: " + serverCardId +
                                ", code: " + response.code());
                        completedCards[0]++;
                        if (completedCards[0] == totalCards) callback.onSuccess();
                        return;
                    }

                    ReviewDto serverReview = response.body();
                    String serverReviewId = serverReview.getId();
                    Log.d(TAG, "Successfully pulled review for serverCardId: " + serverCardId +
                            ", serverReviewId: " + serverReviewId +
                            ", lastReviewed: " + serverReview.getLastReviewed());

                    Integer localReviewId = idMappingRepository.getLocalIdByServerId(serverReviewId, "review");
                    Review localReview = localReviewId != null ? reviewRepository.getReviewById(localReviewId) : null;

                    try {
                        Review existingReview = reviewRepository.getReviewByCardId(card.getId());
                        if (existingReview != null && (localReview == null || existingReview.getId() != localReview.getId())) {
                            reviewRepository.deleteReviewConfirmed(existingReview.getId());
                            Log.d(TAG, "Deleted duplicate review for cardId: " + card.getId() +
                                    ", reviewId: " + existingReview.getId());
                        }

                        String serverLastReviewed = serverReview.getLastReviewed();
                        String localLastReviewed = localReview != null ? localReview.getLastReviewed() : null;

                        if (localReview == null) {
                            Review newReview = new Review();
                            newReview.setServerId(serverReviewId);
                            newReview.setCardId(card.getId());
                            newReview.setEase(serverReview.getEase());
                            newReview.setInterval(serverReview.getInterval());
                            newReview.setRepetition(serverReview.getRepetition());
                            newReview.setNextReviewDate(serverReview.getNextReviewDate());
                            newReview.setLastReviewed(serverLastReviewed);
                            newReview.setLastModified(dateFormat.format(new Date()));
                            newReview.setSyncStatus("synced");
                            long newLocalId = reviewRepository.insertReview(newReview);
                            idMappingRepository.insertIdMapping(new IdMapping((int) newLocalId, serverReviewId, "review"));
                            Log.d(TAG, "Inserted new review with serverId: " + serverReviewId);
                        } else {
                            if ("pending_update".equals(localReview.getSyncStatus())) {
                                Log.d(TAG, "Skipping server update for review with pending_update: " + localReview.getId());
                                completedCards[0]++;
                                if (completedCards[0] == totalCards) callback.onSuccess();
                                return;
                            }
                            if ("pending_delete".equals(localReview.getSyncStatus())) {
                                Log.d(TAG, "Skipping update for review marked as pending_delete: " + localReview.getId());
                                completedCards[0]++;
                                if (completedCards[0] == totalCards) callback.onSuccess();
                                return;
                            }

                            boolean shouldUpdateLocal = false;
                            if (serverLastReviewed == null && localLastReviewed == null) {
                                if (!serverReview.getNextReviewDate().equals(localReview.getNextReviewDate()) ||
                                        serverReview.getEase() != localReview.getEase() ||
                                        serverReview.getInterval() != localReview.getInterval() ||
                                        serverReview.getRepetition() != localReview.getRepetition()) {
                                    shouldUpdateLocal = true;
                                }
                            } else if (serverLastReviewed == null && localLastReviewed != null) {
                                Log.d(TAG, "Server lastReviewed is null, keeping local review: " + localReview.getId());
                            } else if (serverLastReviewed != null && localLastReviewed == null) {
                                shouldUpdateLocal = true;
                            } else {
                                Date serverDate = dateFormat.parse(serverLastReviewed);
                                Date localDate = dateFormat.parse(localLastReviewed);
                                if (serverDate.after(localDate)) {
                                    shouldUpdateLocal = true;
                                }
                            }

                            if (shouldUpdateLocal) {
                                localReview.setEase(serverReview.getEase());
                                localReview.setInterval(serverReview.getInterval());
                                localReview.setRepetition(serverReview.getRepetition());
                                localReview.setNextReviewDate(serverReview.getNextReviewDate());
                                localReview.setLastReviewed(serverLastReviewed);
                                localReview.setLastModified(dateFormat.format(new Date()));
                                reviewRepository.updateReview(localReview, true);
                                reviewRepository.updateSyncStatus(localReview.getId(), "synced");
                                Log.d(TAG, "Updated review with localId: " + localReview.getId());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing review for serverCardId: " + serverCardId + ": " + e.getMessage());
                    }

                    completedCards[0]++;
                    if (completedCards[0] == totalCards) callback.onSuccess();
                }

                @Override
                public void onFailure(Call<ReviewDto> call, Throwable t) {
                    Log.e(TAG, "Network error pulling review for serverCardId: " + serverCardId + ": " + t.getMessage());
                    completedCards[0]++;
                    if (completedCards[0] == totalCards) callback.onSuccess();
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

        pendingReviews.removeIf(review -> {
            Card card = cardRepository.getCardById(review.getCardId());
            if (card == null || "pending_delete".equals(card.getSyncStatus())) {
                if (idMappingRepository.getServerIdByLocalId(review.getCardId(), "card") == null) {
                    reviewRepository.deleteReviewConfirmed(review.getId());
                    Log.d(TAG, "Deleted review for non-synced deleted card - ID: " + review.getId());
                }
                return true;
            }
            return false;
        });

        if (pendingReviews.isEmpty()) {
            Log.d(TAG, "No pending reviews to sync");
            callback.onSuccess();
            return;
        }

        final int totalPending = pendingReviews.size();
        final int[] completedTasks = {0};

        for (Review review : new ArrayList<>(pendingReviews)) {
            switch (review.getSyncStatus()) {
                case "pending_create":
                    createReviewOnServer(review, new SyncCallback() {
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
                    updateReviewOnServer(review, new SyncCallback() {
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
                    deleteReviewOnServer(review, pendingReviews, new SyncCallback() {
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

    private void createReviewOnServer(final Review review, final SyncCallback callback) {
        String serverCardId = idMappingRepository.getServerIdByLocalId(review.getCardId(), "card");
        if (serverCardId == null) {
            Log.d(TAG, "Card not synced for reviewId: " + review.getId() + ", delaying sync");
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

        apiService.createReview(dto).enqueue(new Callback<ReviewDto>() {
            @Override
            public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReviewDto serverReview = response.body();
                    review.setServerId(serverReview.getId());
                    review.setLastModified(dateFormat.format(new Date()));
                    reviewRepository.updateReview(review, true);
                    idMappingRepository.insertIdMapping(new IdMapping(review.getId(), serverReview.getId(), "review"));
                    reviewRepository.updateSyncStatus(review.getId(), "synced");
                    Log.d(TAG, "Created review on server with serverId: " + serverReview.getId());
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to create review: " + response.code());
                    callback.onFailure("Failed to create review: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ReviewDto> call, Throwable t) {
                Log.e(TAG, "Network error creating review: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void updateReviewOnServer(final Review review, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(review.getId(), "review");
        if (serverId == null) {
            createReviewOnServer(review, callback);
            return;
        }

        String serverCardId = idMappingRepository.getServerIdByLocalId(review.getCardId(), "card");
        if (serverCardId == null) {
            Log.d(TAG, "Card not synced for reviewId: " + review.getId() + ", delaying sync");
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

        apiService.updateReview(serverId, dto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    review.setLastModified(dateFormat.format(new Date()));
                    reviewRepository.updateReview(review, true);
                    reviewRepository.updateSyncStatus(review.getId(), "synced");
                    Log.d(TAG, "Updated review on server with serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to update review: " + response.code());
                    callback.onFailure("Failed to update review: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error updating review: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void deleteReviewOnServer(final Review review, final List<Review> pendingReviews, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(review.getId(), "review");
        if (serverId == null) {
            reviewRepository.deleteReviewConfirmed(review.getId());
            pendingReviews.remove(review);
            Log.d(TAG, "No serverId found, deleted review locally: " + review.getId());
            callback.onSuccess();
            return;
        }

        apiService.deleteReview(serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    reviewRepository.deleteReviewConfirmed(review.getId());
                    pendingReviews.remove(review);
                    Log.d(TAG, "Deleted review on server with serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to delete review: " + response.code());
                    callback.onFailure("Failed to delete review: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error deleting review: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void cleanOrphanedReviews() {
        Log.d(TAG, "Cleaning up orphaned reviews...");
        List<Card> cards = cardRepository.getAllCards();
        Set<Integer> cardIds = new HashSet<>();
        for (Card card : cards) {
            cardIds.add(card.getId());
        }

        List<Review> reviews = reviewRepository.getAllReviews();
        for (Review review : reviews) {
            if (!cardIds.contains(review.getCardId())) {
                String serverCardId = idMappingRepository.getServerIdByLocalId(review.getCardId(), "card");
                if (serverCardId == null) {
                    reviewRepository.deleteReviewConfirmed(review.getId());
                    Log.d(TAG, "Deleted orphaned review immediately - ID: " + review.getId());
                } else {
                    reviewRepository.deleteReview(review.getId());
                    Log.d(TAG, "Marked orphaned review for deletion - ID: " + review.getId());
                }
            }
        }
        Log.d(TAG, "Orphaned reviews cleanup completed");
    }

    // SESSION SYNC SECTION
    public void syncSessions(final SyncCallback callback) {
        Log.d(TAG, "Starting session sync...");
        pushSessionsToServer(new SyncCallback() {
            @Override
            public void onSuccess() {
                pullSessionsFromServer(new SyncCallback() {
                    @Override
                    public void onSuccess() {
                        List<LearningSession> pendingSessions = new ArrayList<>();
                        pendingSessions.addAll(sessionRepository.getPendingSessions("pending_create"));
                        pendingSessions.addAll(sessionRepository.getPendingSessions("pending_update"));
                        pendingSessions.addAll(sessionRepository.getPendingSessions("pending_delete"));
                        if (!pendingSessions.isEmpty()) {
                            Log.d(TAG, "Pending sessions remain: " + pendingSessions.size());
                            syncSessions(callback);
                        } else {
                            Log.d(TAG, "Session sync completed successfully");
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

    private void pullSessionsFromServer(final SyncCallback callback) {
        Log.d(TAG, "Pulling sessions from server...");
        List<Desk> desks = deskRepository.getAllDesks();
        if (desks.isEmpty()) {
            Log.d(TAG, "No desks found, skipping session sync");
            callback.onSuccess();
            return;
        }

        final int totalDesks = desks.size();
        final int[] completedDesks = {0};

        for (Desk desk : desks) {
            String serverDeskId = idMappingRepository.getServerIdByLocalId(desk.getId(), "desk");
            if (serverDeskId == null) {
                Log.d(TAG, "Desk not synced, skipping sessions for deskId: " + desk.getId());
                completedDesks[0]++;
                if (completedDesks[0] == totalDesks) callback.onSuccess();
                continue;
            }

            apiService.getSessionsByDeskId(serverDeskId).enqueue(new Callback<List<SessionDto>>() {
                @Override
                public void onResponse(Call<List<SessionDto>> call, Response<List<SessionDto>> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "Failed to pull sessions for deskId: " + serverDeskId +
                                ", code: " + response.code());
                        completedDesks[0]++;
                        if (completedDesks[0] == totalDesks) callback.onSuccess();
                        return;
                    }

                    List<SessionDto> serverSessions = response.body();
                    Log.d(TAG, "Successfully pulled " + serverSessions.size() + " sessions for deskId: " + serverDeskId);

                    for (SessionDto serverSession : serverSessions) {
                        if (serverSession.getId() == null) {
                            Log.e(TAG, "Invalid server session data, skipping for deskId: " + serverDeskId);
                            continue;
                        }

                        String serverSessionId = serverSession.getId();
                        Integer localId = idMappingRepository.getLocalIdByServerId(serverSessionId, "session");
                        LearningSession localSession = localId != null ? sessionRepository.getSessionById(localId) : null;

                        if (localSession == null) {
                            LearningSession newSession = new LearningSession();
                            newSession.setServerId(serverSessionId);
                            newSession.setDeskId(desk.getId());
                            newSession.setStartTime(serverSession.getStartTime());
                            newSession.setEndTime(serverSession.getEndTime());
                            newSession.setCardsStudied(serverSession.getCardsStudied());
                            newSession.setPerformance(serverSession.getPerformance());
                            newSession.setLastModified(dateFormat.format(new Date()));
                            newSession.setSyncStatus("synced");
                            long newLocalId = sessionRepository.insertSession(newSession);
                            idMappingRepository.insertIdMapping(new IdMapping((int) newLocalId, serverSessionId, "session"));
                            Log.d(TAG, "Inserted new session with serverId: " + serverSessionId);
                        } else {
                            String serverEndTime = serverSession.getEndTime();
                            String localEndTime = localSession.getEndTime();
                            if ("pending_delete".equals(localSession.getSyncStatus())) {
                                Log.d(TAG, "Skipping update for session marked as pending_delete: " + localSession.getId());
                                continue;
                            }
                            if (!nullSafeEquals(serverEndTime, localEndTime)) {
                                localSession.setStartTime(serverSession.getStartTime());
                                localSession.setEndTime(serverEndTime);
                                localSession.setCardsStudied(serverSession.getCardsStudied());
                                localSession.setPerformance(serverSession.getPerformance());
                                localSession.setLastModified(dateFormat.format(new Date()));
                                sessionRepository.updateSession(localSession, true);
                                sessionRepository.updateSyncStatus(localSession.getId(), "synced");
                                Log.d(TAG, "Updated session with localId: " + localSession.getId() + " due to endTime mismatch");
                            }
                        }
                    }

                    completedDesks[0]++;
                    if (completedDesks[0] == totalDesks) callback.onSuccess();
                }

                @Override
                public void onFailure(Call<List<SessionDto>> call, Throwable t) {
                    Log.e(TAG, "Network error pulling sessions for deskId: " + serverDeskId + ": " + t.getMessage());
                    completedDesks[0]++;
                    if (completedDesks[0] == totalDesks) callback.onSuccess();
                }
            });
        }
    }

    private void pushSessionsToServer(final SyncCallback callback) {
        Log.d(TAG, "Pushing sessions to server...");
        List<LearningSession> pendingSessions = new ArrayList<>();
        pendingSessions.addAll(sessionRepository.getPendingSessions("pending_create"));
        pendingSessions.addAll(sessionRepository.getPendingSessions("pending_update"));
        pendingSessions.addAll(sessionRepository.getPendingSessions("pending_delete"));

        if (pendingSessions.isEmpty()) {
            Log.d(TAG, "No pending sessions to sync");
            callback.onSuccess();
            return;
        }

        final int totalPending = pendingSessions.size();
        final int[] completedTasks = {0};

        for (LearningSession session : new ArrayList<>(pendingSessions)) {
            switch (session.getSyncStatus()) {
                case "pending_create":
                    createSessionOnServer(session, new SyncCallback() {
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
                    updateSessionOnServer(session, new SyncCallback() {
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
                    deleteSessionOnServer(session, pendingSessions, new SyncCallback() {
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

    private void createSessionOnServer(final LearningSession session, final SyncCallback callback) {
        String serverDeskId = idMappingRepository.getServerIdByLocalId(session.getDeskId(), "desk");
        if (serverDeskId == null) {
            Log.d(TAG, "Desk not synced for sessionId: " + session.getId() + ", delaying sync");
            callback.onSuccess();
            return;
        }

        SessionDto dto = new SessionDto();
        dto.setDeskId(serverDeskId);
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setCardsStudied(session.getCardsStudied());
        dto.setPerformance(session.getPerformance());

        apiService.createSession(dto).enqueue(new Callback<SessionDto>() {
            @Override
            public void onResponse(Call<SessionDto> call, Response<SessionDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SessionDto serverSession = response.body();
                    session.setServerId(serverSession.getId());
                    session.setLastModified(dateFormat.format(new Date()));
                    sessionRepository.updateSession(session, true);
                    idMappingRepository.insertIdMapping(new IdMapping(session.getId(), serverSession.getId(), "session"));
                    sessionRepository.updateSyncStatus(session.getId(), "synced");
                    Log.d(TAG, "Created session on server with serverId: " + serverSession.getId());
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to create session: " + response.code());
                    callback.onFailure("Failed to create session: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<SessionDto> call, Throwable t) {
                Log.e(TAG, "Network error creating session: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void updateSessionOnServer(final LearningSession session, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(session.getId(), "session");
        if (serverId == null) {
            createSessionOnServer(session, callback);
            return;
        }

        String serverDeskId = idMappingRepository.getServerIdByLocalId(session.getDeskId(), "desk");
        if (serverDeskId == null) {
            Log.d(TAG, "Desk not synced for sessionId: " + session.getId() + ", delaying sync");
            callback.onSuccess();
            return;
        }

        SessionDto dto = new SessionDto();
        dto.setDeskId(serverDeskId);
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setCardsStudied(session.getCardsStudied());
        dto.setPerformance(session.getPerformance());

        apiService.updateSession(serverId, dto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    session.setLastModified(dateFormat.format(new Date()));
                    sessionRepository.updateSession(session, true);
                    sessionRepository.updateSyncStatus(session.getId(), "synced");
                    Log.d(TAG, "Updated session on server with serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to update session: " + response.code());
                    callback.onFailure("Failed to update session: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error updating session: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void deleteSessionOnServer(final LearningSession session, final List<LearningSession> pendingSessions, final SyncCallback callback) {
        String serverId = idMappingRepository.getServerIdByLocalId(session.getId(), "session");
        if (serverId == null) {
            sessionRepository.deleteSessionConfirmed(session.getId());
            pendingSessions.remove(session);
            Log.d(TAG, "No serverId found, deleted session locally: " + session.getId());
            callback.onSuccess();
            return;
        }

        apiService.deleteSession(serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    sessionRepository.deleteSessionConfirmed(session.getId());
                    pendingSessions.remove(session);
                    Log.d(TAG, "Deleted session on server with serverId: " + serverId);
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to delete session: " + response.code());
                    callback.onFailure("Failed to delete session: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error deleting session: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private boolean nullSafeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private Date parseFlexibleDate(String dateStr) throws ParseException {
        if (dateStr == null) {
            return new Date();
        }
        try {
            return dateFormat.parse(dateStr);
        } catch (ParseException e) {
            SimpleDateFormat altFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
            try {
                return altFormat.parse(dateStr);
            } catch (ParseException e2) {
                Log.e(TAG, "Unparseable date: " + dateStr);
                throw e2;
            }
        }
    }

    public interface SyncCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
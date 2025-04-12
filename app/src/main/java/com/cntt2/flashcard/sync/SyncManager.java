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
    private final IdMappingRepository idMappingRepository;
    private final ApiService apiService;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat minuteFormat;

    public SyncManager(Context context) {
        this.folderRepository = App.getInstance().getFolderRepository();
        this.deskRepository = App.getInstance().getDeskRepository();
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

    public interface SyncCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
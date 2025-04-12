package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.FolderDao;
import com.cntt2.flashcard.data.local.dao.IdMappingDao;
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

public class FolderRepository {
    private static final String TAG = "FolderRepository";
    private final FolderDao folderDao;
    private final DeskRepository deskRepository;
    private final IdMappingRepository idMappingRepository;
    private final SimpleDateFormat dateFormat;

    public FolderRepository(Context context) {
        folderDao = new FolderDao(context);
        deskRepository = App.getInstance().getDeskRepository();
        idMappingRepository = new IdMappingRepository(context);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public long insertFolder(Folder folder) {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date());
        folder.setLastModified(currentTime);
        if (folder.getCreatedAt() == null) {
            folder.setCreatedAt(currentTime);
        }
        if (folder.getServerId() == null) {
            folder.setSyncStatus("pending_create");
        } else {
            folder.setSyncStatus("synced");
        }

        long localId = folderDao.insertFolder(folder);
        return localId;
    }

    public void updateFolder(Folder folder, boolean fromSync) {
        Folder existingFolder = getFolderById(folder.getId());
        if (existingFolder != null) {
            if (!fromSync && (!existingFolder.getName().equals(folder.getName()) ||
                    !nullSafeEquals(existingFolder.getParentFolderId(), folder.getParentFolderId()))) {
                folder.setLastModified(dateFormat.format(new Date()));
                folder.setSyncStatus("pending_update");
            }
            folderDao.updateFolder(folder); // Luôn cập nhật vào database
        }
    }

    public void deleteFolder(Folder folder) {
        folder.setSyncStatus("pending_delete");
        folderDao.updateFolder(folder);
        for (Folder subFolder : getSubFolders(folder.getId())) {
            deleteFolder(subFolder);
        }
        for (Desk desk : deskRepository.getDesksByFolderId(folder.getId())) {
            deskRepository.deleteDesk(desk);
        }
    }

    public void deleteFolderConfirmed(int folderId) {
        Folder folder = getFolderById(folderId);
        if (folder != null) {
            folderDao.deleteFolder(folderId);
            idMappingRepository.deleteIdMapping(folderId, "folder");
            Log.d(TAG, "Deleted folder with localId: " + folderId);
        }
    }

    public Folder getFolderById(int id) {
        return folderDao.getFolderById(id);
    }

    public List<Folder> getAllFolders() {
        List<Folder> allFolders = folderDao.getAllFolders();
        List<Folder> filteredFolders = new ArrayList<>(allFolders);
        filteredFolders.removeIf(folder -> "pending_delete".equals(folder.getSyncStatus()));
        return new ArrayList<>(filteredFolders);
    }

    public List<Folder> getSubFolders(int parentFolderId) {
        List<Folder> allFolders = getAllFolders();
        List<Folder> subFolders = new ArrayList<>();
        for (Folder folder : allFolders) {
            if (folder.getParentFolderId() != null && folder.getParentFolderId() == parentFolderId) {
                subFolders.add(folder);
            }
        }
        return subFolders;
    }

    public List<Folder> getNestedFolders() {
        List<Folder> folders = folderDao.getAllNestedFolders();
        folders.removeIf(folder -> "pending_delete".equals(folder.getSyncStatus()));
        return new ArrayList<>(folders);
    }

    public List<Folder> getPendingFolders(String syncStatus) {
        List<Folder> allFolders = folderDao.getAllFolders();
        List<Folder> pendingFolders = new ArrayList<>();
        for (Folder folder : allFolders) {
            if (syncStatus.equals(folder.getSyncStatus())) {
                pendingFolders.add(folder);
            }
        }
        return pendingFolders;
    }

    public void updateSyncStatus(int folderId, String syncStatus) {
        Folder folder = getFolderById(folderId);
        if (folder != null) {
            folder.setSyncStatus(syncStatus);
            folderDao.updateFolder(folder);
        }
    }

    private boolean nullSafeEquals(Integer a, Integer b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
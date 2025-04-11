package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.FolderDao;
import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class FolderRepository {
    private final FolderDao folderDao;
    private final DeskRepository deskRepository;
    private final IdMappingDao idMappingDao;
    private final SimpleDateFormat dateFormat;

    public FolderRepository(Context context) {
        folderDao = new FolderDao(context);
        deskRepository = App.getInstance().getDeskRepository();
        idMappingDao = new IdMappingDao(context);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public long insertFolder(Folder folder) {
        String currentTime = dateFormat.format(new Date());
        folder.setLastModified(currentTime);
        if (folder.getCreatedAt() == null) {
            folder.setCreatedAt(currentTime);
        }
        if (folder.getServerId() == null) {
            folder.setSyncStatus("pending_create");
        } else {
            folder.setSyncStatus("synced");
        }
        return folderDao.insertFolder(folder);
    }

    public void updateFolder(Folder folder) {
        Folder existingFolder = folderDao.getFolderById(folder.getId());
        if (existingFolder != null) {
            boolean hasChanges = !existingFolder.getName().equals(folder.getName()) ||
                    !Objects.equals(existingFolder.getParentFolderId(), folder.getParentFolderId());
            if (hasChanges) {
                folder.setLastModified(dateFormat.format(new Date()));
                folder.setSyncStatus("pending_update");
                folderDao.updateFolder(folder);
            }
        }
    }

    public void deleteFolder(Folder folder) {
        List<Desk> desks = deskRepository.getDesksByFolderId(folder.getId());
        for (Desk desk : desks) {
            deskRepository.deleteDesk(desk);
        }

        List<Folder> subFolders = folder.getSubFolders();
        for (Folder subFolder : subFolders) {
            deleteFolder(subFolder);
        }

        folder.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        folder.setSyncStatus("pending_delete");
        folderDao.updateFolder(folder);
    }

    public void deleteFolderConfirmed(int folderId) {
        folderDao.deleteFolder(folderId);
    }

    public List<Folder> getAllFolders() {
        List<Folder> folders = folderDao.getAllFolders();
        folders.removeIf(folder -> "pending_delete".equals(folder.getSyncStatus()));
        return folders;
    }

    public List<Folder> getNestedFolders() {
        List<Folder> folders = folderDao.getAllNestedFolders();
        folders.removeIf(folder -> "pending_delete".equals(folder.getSyncStatus()));
        return folders;
    }

    public List<Folder> getPendingFolders(String syncStatus) {
        return folderDao.getPendingFolders(syncStatus); // Không lọc ở đây vì đây là lấy bản ghi pending
    }

    public void insertIdMapping(long localId, String serverId) {
        folderDao.insertIdMapping(localId, serverId, "folder");
    }

    public Integer getLocalIdByServerId(String serverId) {
        return folderDao.getLocalIdByServerId(serverId, "folder");
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        folderDao.updateSyncStatus(localId, syncStatus);
    }
}
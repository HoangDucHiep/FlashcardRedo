package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.DeskDao;
import com.cntt2.flashcard.data.local.dao.FolderDao;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;

import java.util.List;

public class FolderRepository {
    private FolderDao folderDao;
    private DeskRepository deskRepository = App.getInstance().getDeskRepository();

    public FolderRepository(Context context) {
        folderDao = new FolderDao(context);
    }

    public long insertFolder(Folder folder) {
        return folderDao.insertFolder(folder);
    }

    public void updateFolder(Folder folder) {
        folderDao.updateFolder(folder);
    }

    public void deleteFolder(Folder folder) {
        // delete all nested folders and desks
        List<Desk> desks = deskRepository.getDesksByFolderId(folder.getId());
        for (Desk desk : desks) {
            deskRepository.deleteDesk(desk);
        }

        List<Folder> subFolders = folder.getSubFolders();

        for (Folder subFolder : subFolders) {
            deleteFolder(subFolder);
        }

        folderDao.deleteFolder(folder.getId());
    }

    public List<Folder> getAllFolders() {
        // Lấy danh sách tất cả các folder
        return folderDao.getAllFolders();
    }

    public List<Folder> getNestedFolders() {
        // Lấy danh sách các root folders (đã bao gồm subFolders và desks)
        return folderDao.getAllNestedFolders();
    }


}
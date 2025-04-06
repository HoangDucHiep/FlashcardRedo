package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.data.local.dao.DeskDao;
import com.cntt2.flashcard.data.local.dao.FolderDao;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;

import java.util.List;

public class FolderRepository {
    private FolderDao folderDao;
    private DeskDao deskDao;

    public FolderRepository(Context context) {
        folderDao = new FolderDao(context);
        deskDao = new DeskDao(context);
    }

    public long insertFolder(Folder folder) {
        return folderDao.insertFolder(folder);
    }

    public void updateFolder(Folder folder) {
        folderDao.updateFolder(folder);
    }

    public void deleteFolder(Folder folder) {
        // delete all nested folders and desks
        List<Desk> desks = deskDao.getDesksByFolderId(folder.getId());
        for (Desk desk : desks) {
            deskDao.deleteDesk(desk.getId());
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

    public void seedDatabase() {
        // Xóa dữ liệu hiện có để tránh trùng lặp
        folderDao.clearAllFolders();
        deskDao.clearAllDesks();

        // Tạo thời gian tạo (createdAt)
        String createdAt = "4/5/2025 11:33:00 PM";
        String deskCreatedAt = "4/5/2025 11:46:13 PM";

        // Tạo Root 1 (id: 12)
        Folder root1 = new Folder();
        root1.setId(12);
        root1.setName("Root 1");
        root1.setCreatedAt(createdAt);
        root1.setParentFolderId(null);
        folderDao.insertFolder(root1);

        // Tạo Root 2 (id: 13)
        Folder root2 = new Folder();
        root2.setId(13);
        root2.setName("Root 2");
        root2.setCreatedAt(createdAt);
        root2.setParentFolderId(null);
        folderDao.insertFolder(root2);

        // Tạo Root 3 (id: 14)
        Folder root3 = new Folder();
        root3.setId(14);
        root3.setName("Root 3");
        root3.setCreatedAt(createdAt);
        root3.setParentFolderId(null);
        folderDao.insertFolder(root3);

        // Tạo Folder 1 (id: 15, con của Root 1)
        Folder folder1 = new Folder();
        folder1.setId(15);
        folder1.setName("Folder 1");
        folder1.setCreatedAt(createdAt);
        folder1.setParentFolderId(12);
        folderDao.insertFolder(folder1);

        // Tạo Folder 2 (id: 16, con của Root 1)
        Folder folder2 = new Folder();
        folder2.setId(16);
        folder2.setName("Folder 2");
        folder2.setCreatedAt(createdAt);
        folder2.setParentFolderId(12);
        folderDao.insertFolder(folder2);

        // Tạo Folder 3 (id: 17, con của Root 1)
        Folder folder3 = new Folder();
        folder3.setId(17);
        folder3.setName("Folder 3");
        folder3.setCreatedAt(createdAt);
        folder3.setParentFolderId(12);
        folderDao.insertFolder(folder3);

        // Tạo Folder 1.1 (id: 18, con của Folder 1)
        Folder folder1_1 = new Folder();
        folder1_1.setId(18);
        folder1_1.setName("Folder 1.1");
        folder1_1.setCreatedAt(createdAt);
        folder1_1.setParentFolderId(15);
        folderDao.insertFolder(folder1_1);

        // Tạo Folder 1.2 (id: 19, con của Folder 1)
        Folder folder1_2 = new Folder();
        folder1_2.setId(19);
        folder1_2.setName("Folder 1.2");
        folder1_2.setCreatedAt(createdAt);
        folder1_2.setParentFolderId(15);
        folderDao.insertFolder(folder1_2);

        // Tạo Folder 1.3 (id: 20, con của Folder 1)
        Folder folder1_3 = new Folder();
        folder1_3.setId(20);
        folder1_3.setName("Folder 1.3");
        folder1_3.setCreatedAt(createdAt);
        folder1_3.setParentFolderId(15);
        folderDao.insertFolder(folder1_3);

        // Tạo Desk 1 (id: 1, thuộc Root 1)
        Desk desk1 = new Desk();
        desk1.setId(1);
        desk1.setFolderId(12);
        desk1.setName("Desk 1");
        desk1.setCreatedAt(deskCreatedAt);
        desk1.setPublic(false);
        deskDao.insertDesk(desk1);
    }
}
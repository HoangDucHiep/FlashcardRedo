package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;
import com.cntt2.flashcard.model.IdMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FolderDao {
    private DatabaseHelper dbHelper;
    private DeskDao deskDao;
    private final IdMappingDao idMappingDao;

    public FolderDao(Context context) {
        dbHelper = new DatabaseHelper(context);
        deskDao = new DeskDao(context);
        idMappingDao = new IdMappingDao(context);
    }

    public long insertFolder(Folder folder) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("parent_folder_id", folder.getParentFolderId());
        values.put("name", folder.getName());
        values.put("created_at", folder.getCreatedAt());
        values.put("last_modified", folder.getLastModified());
        values.put("sync_status", folder.getSyncStatus());
        long id = db.insert("folders", null, values);
        db.close();
        return id;
    }

    public int deleteFolder(int folderId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete("folders", "id = ?", new String[]{String.valueOf(folderId)});
        db.close();
        return rowsAffected;
    }

    public void updateFolder(Folder folder) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (folder.getParentFolderId() != null) {
            values.put("parent_folder_id", folder.getParentFolderId());
        }
        values.put("parent_folder_id", folder.getParentFolderId());
        values.put("name", folder.getName());
        values.put("created_at", folder.getCreatedAt());
        values.put("last_modified", folder.getLastModified());
        values.put("sync_status", folder.getSyncStatus());
        db.update("folders", values, "id = ?", new String[]{String.valueOf(folder.getId())});
        db.close();
    }

    public void clearAllFolders() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM folders");
        db.close();
    }

    public Folder getFolderById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("folders", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);
        Folder folder = null;
        if (cursor != null && cursor.moveToFirst()) {
            folder = new Folder();
            folder.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            folder.setParentFolderId(cursor.isNull(cursor.getColumnIndexOrThrow("parent_folder_id")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("parent_folder_id")));
            folder.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            folder.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
            folder.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
            folder.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
            cursor.close();
        }
        db.close();
        return folder;
    }

    public List<Folder> getAllFolders() {
        List<Folder> folders = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM folders", null);
        if (cursor.moveToFirst()) {
            do {
                Folder folder = new Folder();
                folder.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                folder.setParentFolderId(cursor.isNull(cursor.getColumnIndexOrThrow("parent_folder_id")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("parent_folder_id")));
                folder.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                folder.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                folder.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                folder.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                folders.add(folder);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return folders;
    }

    @SuppressLint({"Range", "NewApi"})
    public List<Folder> getAllNestedFolders() {
        List<Folder> allFolders = new ArrayList<>();
        Map<Integer, Folder> folderMap = new HashMap<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM folders", null);
        if (cursor.moveToFirst()) {
            do {
                Folder folder = new Folder();
                folder.setId(cursor.getInt(cursor.getColumnIndex("id")));
                int parentFolderId = cursor.isNull(cursor.getColumnIndex("parent_folder_id")) ? -1 : cursor.getInt(cursor.getColumnIndex("parent_folder_id"));
                folder.setParentFolderId(parentFolderId == -1 ? null : parentFolderId);
                folder.setName(cursor.getString(cursor.getColumnIndex("name")));
                folder.setCreatedAt(cursor.getString(cursor.getColumnIndex("created_at")));
                folder.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                folder.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                folderMap.put(folder.getId(), folder);
                allFolders.add(folder);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Remove folders with sync status "pending_delete"
        allFolders.removeIf(folder -> "pending_delete".equals(folder.getSyncStatus()));
        allFolders = new ArrayList<>(allFolders);

        // Build the hierarchy
        for (Folder folder : allFolders) {
            if (folder.getParentFolderId() != null && !Objects.equals(folder.getSyncStatus(), "pending_delete")) {
                Folder parentFolder = folderMap.get(folder.getParentFolderId());
                if (parentFolder != null) {
                    parentFolder.addSubFolder(folder);
                }
            }
            // Load desks for each folder
            folder.setDesks(deskDao.getDesksByFolderId(folder.getId()).stream().filter(desk -> !desk.getSyncStatus().equals("pending_delete")).toList());
        }

        // Filter root folders
        List<Folder> rootFolders = new ArrayList<>();
        for (Folder folder : allFolders) {
            if (folder.getParentFolderId() == null) {
                rootFolders.add(folder);
            }
        }

        db.close();
        return rootFolders;
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sync_status", syncStatus);
        db.update("folders", values, "id = ?", new String[]{String.valueOf(localId)});
        db.close();
    }
}
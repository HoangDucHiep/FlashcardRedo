package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.IdMapping;

import java.util.ArrayList;
import java.util.List;

public class DeskDao {
    private DatabaseHelper dbHelper;
    private CardDao cardDao;
    private IdMappingDao idMappingDao;

    public DeskDao(Context context) {
        dbHelper = new DatabaseHelper(context);
        cardDao = new CardDao(context);
        idMappingDao = new IdMappingDao(context);
    }

    public long insertDesk(Desk desk) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        //values.put("id", desk.getId());
        values.put("folder_id", desk.getFolderId());
        values.put("name", desk.getName());
        values.put("created_at", desk.getCreatedAt());
        values.put("is_public", desk.isPublic() ? 1 : 0);
        values.put("last_modified", desk.getLastModified());
        values.put("sync_status", desk.getSyncStatus());
        long id = db.insert("desks", null, values);
        db.close();
        return id;
    }

    public int deleteDesk(int deskId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = 0;
        try {
            db.beginTransaction();
            rowsAffected = db.delete("desks", "id = ?", new String[]{String.valueOf(deskId)});
            db.setTransactionSuccessful();
            Log.d("DeskDao", "Deleted desk - ID: " + deskId + ", rowsAffected: " + rowsAffected);
        } catch (Exception e) {
            Log.e("DeskDao", "Failed to delete desk - ID: " + deskId + ", error: " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
        return rowsAffected;
    }

    public void clearAllDesks() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM desks");
        db.close();
    }

    public void updateDesk(Desk desk) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("folder_id", desk.getFolderId());
        values.put("name", desk.getName());
        values.put("is_public", desk.isPublic() ? 1 : 0);
        values.put("created_at", desk.getCreatedAt());
        values.put("last_modified", desk.getLastModified());
        values.put("sync_status", desk.getSyncStatus());
        db.update("desks", values, "id = ?", new String[]{String.valueOf(desk.getId())});
        db.close();
    }

    @SuppressLint("Range")
    public Desk getDeskById(int id) {
        Desk desk = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM desks WHERE id = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            desk = new Desk();
            desk.setId(cursor.getInt(cursor.getColumnIndex("id")));
            desk.setFolderId(cursor.getInt(cursor.getColumnIndex("folder_id")));
            desk.setName(cursor.getString(cursor.getColumnIndex("name")));
            desk.setCreatedAt(cursor.getString(cursor.getColumnIndex("created_at")));
            desk.setPublic(cursor.getInt(cursor.getColumnIndex("is_public")) == 1);
            desk.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
            desk.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
            desk.setCards(cardDao.getCardsByDeskId(desk.getId()));
        }
        cursor.close();
        db.close();
        return desk;
    }

    @SuppressLint("Range")
    public List<Desk> getDesksByFolderId(int folderId) {
        List<Desk> desks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM desks WHERE folder_id = ?", new String[]{String.valueOf(folderId)});
        if (cursor.moveToFirst()) {
            do {
                Desk desk = new Desk();
                desk.setId(cursor.getInt(cursor.getColumnIndex("id")));
                desk.setFolderId(cursor.getInt(cursor.getColumnIndex("folder_id")));
                desk.setName(cursor.getString(cursor.getColumnIndex("name")));
                desk.setCreatedAt(cursor.getString(cursor.getColumnIndex("created_at")));
                desk.setPublic(cursor.getInt(cursor.getColumnIndex("is_public")) == 1);
                desk.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                desk.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                desk.setCards(cardDao.getCardsByDeskId(desk.getId()));
                desks.add(desk);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return desks;
    }

    @SuppressLint("Range")
    public List<Desk> getAllDesks() {
        List<Desk> desks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM desks", null);
        if (cursor.moveToFirst()) {
            do {
                Desk desk = new Desk();
                desk.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                desk.setFolderId(cursor.isNull(cursor.getColumnIndexOrThrow("folder_id")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("folder_id")));
                desk.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                desk.setPublic(cursor.getInt(cursor.getColumnIndexOrThrow("is_public")) == 1);
                desk.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                desk.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                desk.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                desks.add(desk);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return desks;
    }


    public void updateSyncStatus(int localId, String syncStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sync_status", syncStatus);
        db.update("desks", values, "id = ?", new String[]{String.valueOf(localId)});
        db.close();
    }
}
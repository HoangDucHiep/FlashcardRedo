package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Desk;

import java.util.ArrayList;
import java.util.List;

public class DeskDao {
    private DatabaseHelper dbHelper;
    private CardDao cardDao;

    public DeskDao(Context context) {
        dbHelper = new DatabaseHelper(context);
        cardDao = new CardDao(context);
    }

    public long insertDesk(Desk desk) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        //values.put("id", desk.getId());
        values.put("folder_id", desk.getFolderId());
        values.put("name", desk.getName());
        values.put("created_at", desk.getCreatedAt());
        values.put("is_public", desk.isPublic() ? 1 : 0);
        long id = db.insert("desks", null, values);
        db.close();
        return id;
    }

    public void deleteDesk(int deskId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("desks", "id = ?", new String[]{String.valueOf(deskId)});
        db.close();
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
        values.put("created_at", desk.getCreatedAt());
        values.put("is_public", desk.isPublic() ? 1 : 0);
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
                desk.setId(cursor.getInt(cursor.getColumnIndex("id")));
                desk.setFolderId(cursor.getInt(cursor.getColumnIndex("folder_id")));
                desk.setName(cursor.getString(cursor.getColumnIndex("name")));
                desk.setCreatedAt(cursor.getString(cursor.getColumnIndex("created_at")));
                desk.setPublic(cursor.getInt(cursor.getColumnIndex("is_public")) == 1);
                desk.setCards(cardDao.getCardsByDeskId(desk.getId()));
                desks.add(desk);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return desks;
    }
}
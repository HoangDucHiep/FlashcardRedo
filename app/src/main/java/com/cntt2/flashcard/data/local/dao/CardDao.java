package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.IdMapping;

import java.util.ArrayList;
import java.util.List;

public class CardDao {
    private DatabaseHelper dbHelper;
    private IdMappingDao idMappingDao;

    public CardDao(Context context) {
        dbHelper = new DatabaseHelper(context);
        idMappingDao = new IdMappingDao(context);
    }


    public long insertCard(Card card) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("desk_id", card.getDeskId());
        values.put("front", card.getFront());
        values.put("back", card.getBack());
        values.put("created_at", card.getCreatedAt());
        values.put("last_modified", card.getLastModified());
        values.put("sync_status", card.getSyncStatus());
        long id = db.insert("cards", null, values);
        db.close();
        return id;
    }

    public void updateCard(Card card) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("desk_id", card.getDeskId());
        values.put("front", card.getFront());
        values.put("back", card.getBack());
        values.put("created_at", card.getCreatedAt());
        values.put("last_modified", card.getLastModified());
        values.put("sync_status", card.getSyncStatus());
        db.update("cards", values, "id = ?", new String[]{String.valueOf(card.getId())});
        db.close();
    }

    public int deleteCard(int cardId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete("cards", "id = ?", new String[]{String.valueOf(cardId)});
        db.close();
        return rowsAffected;
    }

    @SuppressLint("Range")
    public List<Card> getCardsByDeskId(int deskId) {
        List<Card> cards = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cards WHERE desk_id = ?", new String[]{String.valueOf(deskId)});
        if (cursor.moveToFirst()) {
            do {
                Card card = new Card();
                card.setId(cursor.getInt(cursor.getColumnIndex("id")));
                card.setDeskId(cursor.getInt(cursor.getColumnIndex("desk_id")));
                card.setFront(cursor.getString(cursor.getColumnIndex("front")));
                card.setBack(cursor.getString(cursor.getColumnIndex("back")));
                card.setCreatedAt(cursor.getString(cursor.getColumnIndex("created_at")));
                card.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                card.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                cards.add(card);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return cards;
    }

    @SuppressLint("Range")
    public Card getCardById(int cardId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cards WHERE id = ?", new String[]{String.valueOf(cardId)});
        Card card = null;
        if (cursor.moveToFirst()) {
            card = new Card();
            card.setId(cursor.getInt(cursor.getColumnIndex("id")));
            card.setDeskId(cursor.getInt(cursor.getColumnIndex("desk_id")));
            card.setFront(cursor.getString(cursor.getColumnIndex("front")));
            card.setBack(cursor.getString(cursor.getColumnIndex("back")));
            card.setCreatedAt(cursor.getString(cursor.getColumnIndex("created_at")));
            card.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
            card.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
        }
        cursor.close();
        db.close();
        return card;
    }

    @SuppressLint("Range")
    public List<Card> getAllCards() {
        List<Card> cards = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cards", null);
        if (cursor.moveToFirst()) {
            do {
                Card card = new Card();
                card.setId(cursor.getInt(cursor.getColumnIndex("id")));
                card.setDeskId(cursor.getInt(cursor.getColumnIndex("desk_id")));
                card.setFront(cursor.getString(cursor.getColumnIndex("front")));
                card.setBack(cursor.getString(cursor.getColumnIndex("back")));
                card.setCreatedAt(cursor.getString(cursor.getColumnIndex("created_at")));
                card.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                card.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                cards.add(card);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return cards;
    }

    public List<Card> getPendingCards(String syncStatus) {
        List<Card> cards = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cards WHERE sync_status = ?", new String[]{syncStatus});
        if (cursor.moveToFirst()) {
            do {
                Card card = new Card();
                card.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                card.setDeskId(cursor.getInt(cursor.getColumnIndexOrThrow("desk_id")));
                card.setFront(cursor.getString(cursor.getColumnIndexOrThrow("front")));
                card.setBack(cursor.getString(cursor.getColumnIndexOrThrow("back")));
                card.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                card.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                card.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                cards.add(card);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return cards;
    }

    public void insertIdMapping(long localId, String serverId, String entityType) {
        idMappingDao.insertIdMapping(new IdMapping((int) localId, serverId, entityType));
    }

    public Integer getLocalIdByServerId(String serverId, String entityType) {
        return idMappingDao.getLocalIdByServerId(serverId, entityType);
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sync_status", syncStatus);
        db.update("cards", values, "id = ?", new String[]{String.valueOf(localId)});
        db.close();
    }

}
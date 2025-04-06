package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.Card;

import java.util.ArrayList;
import java.util.List;

public class CardDao {
    private DatabaseHelper dbHelper;

    public CardDao(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long insertCard(Card card) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("desk_id", card.getDeskId());
        values.put("front", card.getFront());
        values.put("back", card.getBack());
        values.put("created_at", card.getCreatedAt());
        long id = db.insert("cards", null, values);
        db.close();
        return id;
    }

    public void deleteCard(int cardId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("cards", "id = ?", new String[]{String.valueOf(cardId)});
        db.close();
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
                cards.add(card);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return cards;
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
                cards.add(card);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return cards;
    }
}
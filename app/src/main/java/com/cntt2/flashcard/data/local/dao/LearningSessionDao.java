package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.LearningSession;
import com.cntt2.flashcard.model.Review;

import java.util.ArrayList;
import java.util.List;
public class LearningSessionDao {
    private DatabaseHelper dbHelper;

    public LearningSessionDao(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long insertSession(LearningSession session) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("desk_id", session.getDeskId());
        values.put("start_time", session.getStartTime());
        values.put("end_time", session.getEndTime());
        values.put("cards_studied", session.getCardsStudied());
        values.put("performance", session.getPerformance());
        long id = db.insert("learning_sessions", null, values);
        db.close();
        return id;
    }

    @SuppressLint("Range")
    public List<LearningSession> getSessionsByDeskId(int deskId) {
        List<LearningSession> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM learning_sessions WHERE desk_id = ?", new String[]{String.valueOf(deskId)});
        if (cursor.moveToFirst()) {
            do {
                LearningSession session = new LearningSession();
                session.setId(cursor.getInt(cursor.getColumnIndex("id")));
                session.setDeskId(cursor.getInt(cursor.getColumnIndex("desk_id")));
                session.setStartTime(cursor.getString(cursor.getColumnIndex("start_time")));
                session.setEndTime(cursor.getString(cursor.getColumnIndex("end_time")));
                session.setCardsStudied(cursor.getInt(cursor.getColumnIndex("cards_studied")));
                session.setPerformance(cursor.getDouble(cursor.getColumnIndex("performance")));
                sessions.add(session);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessions;
    }
}
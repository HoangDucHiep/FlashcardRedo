package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.LearningSession;

import java.util.ArrayList;
import java.util.List;
public class LearningSessionDao {
    private DatabaseHelper dbHelper;

    public LearningSessionDao(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long insertLearningSession(LearningSession session) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("desk_id", session.getDeskId());
        values.put("start_time", session.getStartTime());
        values.put("end_time", session.getEndTime());
        values.put("cards_studied", session.getCardsStudied());
        values.put("performance", session.getPerformance());
        values.put("last_modified", session.getLastModified());
        values.put("sync_status", session.getSyncStatus());
        long id = db.insert("learning_sessions", null, values);
        db.close();
        return id;
    }

    @SuppressLint("Range")
    public LearningSession getSessionById(int id) {
        LearningSession session = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM learning_sessions WHERE id = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            session = new LearningSession();
            session.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            session.setDeskId(cursor.getInt(cursor.getColumnIndexOrThrow("desk_id")));
            session.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow("start_time")));
            session.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow("end_time")));
            session.setCardsStudied(cursor.getInt(cursor.getColumnIndexOrThrow("cards_studied")));
            session.setPerformance(cursor.getDouble(cursor.getColumnIndexOrThrow("performance")));
            session.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
            session.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
        }
        cursor.close();
        db.close();
        return session;
    }

    @SuppressLint("Range")
    public List<LearningSession> getAllSessions() {
        List<LearningSession> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM learning_sessions", null);
        if (cursor.moveToFirst()) {
            do {
                LearningSession session = new LearningSession();
                session.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                session.setDeskId(cursor.getInt(cursor.getColumnIndexOrThrow("desk_id")));
                session.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow("start_time")));
                session.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow("end_time")));
                session.setCardsStudied(cursor.getInt(cursor.getColumnIndexOrThrow("cards_studied")));
                session.setPerformance(cursor.getDouble(cursor.getColumnIndexOrThrow("performance")));
                session.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                session.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                sessions.add(session);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessions;
    }

    public void updateLearningSession(LearningSession session) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("desk_id", session.getDeskId());
        values.put("start_time", session.getStartTime());
        values.put("end_time", session.getEndTime());
        values.put("cards_studied", session.getCardsStudied());
        values.put("performance", session.getPerformance());
        values.put("last_modified", session.getLastModified());
        values.put("sync_status", session.getSyncStatus());
        db.update("learning_sessions", values, "id = ?", new String[]{String.valueOf(session.getId())});
        db.close();
    }

    @SuppressLint("Range")
    public List<LearningSession> getSessionsByDeskId(int deskId) {
        List<LearningSession> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM learning_sessions WHERE desk_id = ?", new String[]{String.valueOf(deskId)});
        if (cursor.moveToFirst()) {
            do {
                LearningSession session = new LearningSession();
                session.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                session.setDeskId(cursor.getInt(cursor.getColumnIndexOrThrow("desk_id")));
                session.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow("start_time")));
                session.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow("end_time")));
                session.setCardsStudied(cursor.getInt(cursor.getColumnIndexOrThrow("cards_studied")));
                session.setPerformance(cursor.getDouble(cursor.getColumnIndexOrThrow("performance")));
                session.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                session.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                sessions.add(session);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessions;
    }

    public int deleteLearningSession(int sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = 0;
        try {
            db.beginTransaction();
            rowsAffected = db.delete("learning_sessions", "id = ?", new String[]{String.valueOf(sessionId)});
            db.setTransactionSuccessful();
            Log.d("LearningSessionDao", "Deleted session - ID: " + sessionId + ", rowsAffected: " + rowsAffected);
        } catch (Exception e) {
            Log.e("LearningSessionDao", "Failed to delete session - ID: " + sessionId + ", error: " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
        return rowsAffected;
    }

    public List<LearningSession> getPendingSessions(String syncStatus) {
        List<LearningSession> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM learning_sessions WHERE sync_status = ?", new String[]{syncStatus});
        if (cursor.moveToFirst()) {
            do {
                LearningSession session = new LearningSession();
                session.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                session.setDeskId(cursor.getInt(cursor.getColumnIndexOrThrow("desk_id")));
                session.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow("start_time")));
                session.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow("end_time")));
                session.setCardsStudied(cursor.getInt(cursor.getColumnIndexOrThrow("cards_studied")));
                session.setPerformance(cursor.getDouble(cursor.getColumnIndexOrThrow("performance")));
                session.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                session.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                sessions.add(session);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessions;
    }


    public void updateSyncStatus(int localId, String syncStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sync_status", syncStatus);
        db.update("learning_sessions", values, "id = ?", new String[]{String.valueOf(localId)});
        db.close();
    }
}
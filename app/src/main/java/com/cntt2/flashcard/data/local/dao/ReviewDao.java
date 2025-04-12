package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.Review;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewDao {
    private DatabaseHelper dbHelper;
    private IdMappingDao idMappingDao;

    public ReviewDao(Context context) {
        dbHelper = new DatabaseHelper(context);
        idMappingDao = new IdMappingDao(context);
    }

    public long insertReview(Review review) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("card_id", review.getCardId());
        values.put("ease", review.getEase());
        values.put("interval", review.getInterval());
        values.put("repetition", review.getRepetition());
        values.put("next_review_date", review.getNextReviewDate());
        values.put("last_reviewed", review.getLastReviewed());
        values.put("last_modified", review.getLastModified());
        values.put("sync_status", review.getSyncStatus());
        long id = db.insert("reviews", null, values);
        db.close();
        Log.d("ReviewDao", "Inserted review - ID: " + id);
        return id;
    }

    public void updateReview(Review review) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("card_id", review.getCardId());
        values.put("ease", review.getEase());
        values.put("interval", review.getInterval());
        values.put("repetition", review.getRepetition());
        values.put("next_review_date", review.getNextReviewDate());
        values.put("last_reviewed", review.getLastReviewed());
        values.put("last_modified", review.getLastModified());
        values.put("sync_status", review.getSyncStatus());
        db.update("reviews", values, "id = ?", new String[]{String.valueOf(review.getId())});
        db.close();
        Log.d("ReviewDao", "Updated review - ID: " + review.getId());
    }

    public int deleteReview(int reviewId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = 0;
        try {
            db.beginTransaction();
            rowsAffected = db.delete("reviews", "id = ?", new String[]{String.valueOf(reviewId)});
            db.setTransactionSuccessful();
            Log.d("ReviewDao", "Deleted review - ID: " + reviewId + ", rowsAffected: " + rowsAffected);
        } catch (Exception e) {
            Log.e("ReviewDao", "Failed to delete review - ID: " + reviewId + ", error: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
            db.close();
        }
        return rowsAffected;
    }

    public Review getReviewById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM reviews WHERE id = ?", new String[]{String.valueOf(id)});
        Review review = null;
        if (cursor.moveToFirst()) {
            review = new Review();
            review.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            review.setCardId(cursor.getInt(cursor.getColumnIndexOrThrow("card_id")));
            review.setEase(cursor.getDouble(cursor.getColumnIndexOrThrow("ease")));
            review.setInterval(cursor.getInt(cursor.getColumnIndexOrThrow("interval")));
            review.setRepetition(cursor.getInt(cursor.getColumnIndexOrThrow("repetition")));
            review.setNextReviewDate(cursor.getString(cursor.getColumnIndexOrThrow("next_review_date")));
            review.setLastReviewed(cursor.getString(cursor.getColumnIndexOrThrow("last_reviewed")));
            review.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
            review.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
        }
        cursor.close();
        db.close();
        return review;
    }

    public List<Review> getAllReviews() {
        List<Review> reviews = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM reviews", null);
        if (cursor.moveToFirst()) {
            do {
                Review review = new Review();
                review.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                review.setCardId(cursor.getInt(cursor.getColumnIndexOrThrow("card_id")));
                review.setEase(cursor.getDouble(cursor.getColumnIndexOrThrow("ease")));
                review.setInterval(cursor.getInt(cursor.getColumnIndexOrThrow("interval")));
                review.setRepetition(cursor.getInt(cursor.getColumnIndexOrThrow("repetition")));
                review.setNextReviewDate(cursor.getString(cursor.getColumnIndexOrThrow("next_review_date")));
                review.setLastReviewed(cursor.getString(cursor.getColumnIndexOrThrow("last_reviewed")));
                review.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                review.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                reviews.add(review);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return reviews;
    }

    public List<Review> getReviewsByCardId(int cardId) {
        List<Review> reviews = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM reviews WHERE card_id = ?", new String[]{String.valueOf(cardId)});
        if (cursor.moveToFirst()) {
            do {
                Review review = new Review();
                review.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                review.setCardId(cursor.getInt(cursor.getColumnIndexOrThrow("card_id")));
                review.setEase(cursor.getDouble(cursor.getColumnIndexOrThrow("ease")));
                review.setInterval(cursor.getInt(cursor.getColumnIndexOrThrow("interval")));
                review.setRepetition(cursor.getInt(cursor.getColumnIndexOrThrow("repetition")));
                review.setNextReviewDate(cursor.getString(cursor.getColumnIndexOrThrow("next_review_date")));
                review.setLastReviewed(cursor.getString(cursor.getColumnIndexOrThrow("last_reviewed")));
                review.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                review.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                reviews.add(review);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return reviews;
    }

    @SuppressLint("Range")
    public Review getReviewByCardId(int cardId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM reviews WHERE card_id = ?", new String[]{String.valueOf(cardId)});
        Review review = null;
        if (cursor.moveToFirst()) {
            review = new Review();
            review.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            review.setCardId(cursor.getInt(cursor.getColumnIndexOrThrow("card_id")));
            review.setEase(cursor.getDouble(cursor.getColumnIndexOrThrow("ease")));
            review.setInterval(cursor.getInt(cursor.getColumnIndexOrThrow("interval")));
            review.setRepetition(cursor.getInt(cursor.getColumnIndexOrThrow("repetition")));
            review.setNextReviewDate(cursor.getString(cursor.getColumnIndexOrThrow("next_review_date")));
            review.setLastReviewed(cursor.getString(cursor.getColumnIndexOrThrow("last_reviewed")));
            review.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
            review.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
        }
        cursor.close();
        db.close();
        return review;
    }

    @SuppressLint("Range")
    public List<Card> getCardsToReview(int deskId) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Card> cards = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT c.* FROM cards c JOIN reviews r ON c.id = r.card_id " +
                        "WHERE c.desk_id = ? AND r.next_review_date <= ? AND r.interval > 0 " +
                        "AND c.sync_status != 'pending_delete'",
                new String[]{String.valueOf(deskId), currentDate}
        );
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

    @SuppressLint("Range")
    public List<Card> getNewCards(int deskId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Card> cards = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT c.* FROM cards c JOIN reviews r ON c.id = r.card_id " +
                        "WHERE c.desk_id = ? AND r.interval = 0 AND c.sync_status != 'pending_delete'",
                new String[]{String.valueOf(deskId)}
        );
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

    public int deleteReviewByCardId(int cardId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = db.delete("reviews", "card_id = ?", new String[]{String.valueOf(cardId)});
        db.close();
        Log.d("ReviewDao", "Deleted reviews for cardId: " + cardId + ", rows: " + deletedRows);
        return deletedRows;
    }

    public List<Review> getPendingReviews(String syncStatus) {
        List<Review> reviews = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM reviews WHERE sync_status = ?", new String[]{syncStatus});
        if (cursor.moveToFirst()) {
            do {
                Review review = new Review();
                review.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                review.setCardId(cursor.getInt(cursor.getColumnIndexOrThrow("card_id")));
                review.setEase(cursor.getDouble(cursor.getColumnIndexOrThrow("ease")));
                review.setInterval(cursor.getInt(cursor.getColumnIndexOrThrow("interval")));
                review.setRepetition(cursor.getInt(cursor.getColumnIndexOrThrow("repetition")));
                review.setNextReviewDate(cursor.getString(cursor.getColumnIndexOrThrow("next_review_date")));
                review.setLastReviewed(cursor.getString(cursor.getColumnIndexOrThrow("last_reviewed")));
                review.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
                review.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow("sync_status")));
                reviews.add(review);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        Log.d("ReviewDao", "Retrieved " + reviews.size() + " pending reviews with syncStatus: " + syncStatus);
        return reviews;
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sync_status", syncStatus);
        db.update("reviews", values, "id = ?", new String[]{String.valueOf(localId)});
        db.close();
        Log.d("ReviewDao", "Updated syncStatus for review - ID: " + localId + ", syncStatus: " + syncStatus);
    }
}
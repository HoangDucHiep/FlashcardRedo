package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewDao {
    private DatabaseHelper dbHelper;

    public ReviewDao(Context context) {
        dbHelper = new DatabaseHelper(context);
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
        long id = db.insert("reviews", null, values);
        db.close();
        return id;
    }

    public void updateReview(Review review) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("ease", review.getEase());
        values.put("interval", review.getInterval());
        values.put("repetition", review.getRepetition());
        values.put("next_review_date", review.getNextReviewDate());
        values.put("last_reviewed", review.getLastReviewed());
        db.update("reviews", values, "id = ?", new String[]{String.valueOf(review.getId())});
        db.close();
    }

    @SuppressLint("Range")
    public List<Review> getReviewsDueToday(int deskId, String today) {
        List<Review> reviews = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT r.* FROM reviews r " +
                        "JOIN cards c ON r.card_id = c.id " +
                        "WHERE c.desk_id = ? AND r.next_review_date <= ?",
                new String[]{String.valueOf(deskId), today});
        if (cursor.moveToFirst()) {
            do {
                Review review = new Review();
                review.setId(cursor.getInt(cursor.getColumnIndex("id")));
                review.setCardId(cursor.getInt(cursor.getColumnIndex("card_id")));
                review.setEase(cursor.getDouble(cursor.getColumnIndex("ease")));
                review.setInterval(cursor.getInt(cursor.getColumnIndex("interval")));
                review.setRepetition(cursor.getInt(cursor.getColumnIndex("repetition")));
                review.setNextReviewDate(cursor.getString(cursor.getColumnIndex("next_review_date")));
                review.setLastReviewed(cursor.getString(cursor.getColumnIndex("last_reviewed")));
                reviews.add(review);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return reviews;
    }
}

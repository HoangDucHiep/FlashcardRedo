package com.cntt2.flashcard.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Review;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    public Review getReviewByCardId(int cardId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM reviews WHERE card_id = ?", new String[]{String.valueOf(cardId)});
        Review review = null;
        if (cursor.moveToFirst()) {
            review = new Review();
            review.setId(cursor.getInt(cursor.getColumnIndex("id")));
            review.setCardId(cursor.getInt(cursor.getColumnIndex("card_id")));
            review.setEase(cursor.getFloat(cursor.getColumnIndex("ease")));
            review.setInterval(cursor.getInt(cursor.getColumnIndex("interval")));
            review.setRepetition(cursor.getInt(cursor.getColumnIndex("repetition")));
            review.setNextReviewDate(cursor.getString(cursor.getColumnIndex("next_review_date")));
            review.setLastReviewed(cursor.getString(cursor.getColumnIndex("last_reviewed")));
        }
        cursor.close();
        db.close();
        return review;
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

    @SuppressLint("Range")
    public List<Card> getCardsToReview(int deskId) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Card> cards = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT c.* FROM cards c JOIN reviews r ON c.id = r.card_id WHERE c.desk_id = ? AND r.next_review_date <= ? AND r.interval > 0",
                new String[]{String.valueOf(deskId), currentDate}
        );
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
    public List<Card> getNewCards(int deskId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Card> cards = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT c.* FROM cards c JOIN reviews r ON c.id = r.card_id WHERE c.desk_id = ? AND r.interval = 0",
                new String[]{String.valueOf(deskId)}
        );
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

    public int deleteReviewByCardId(int cardId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = db.delete("reviews", "card_id = ?", new String[]{String.valueOf(cardId)});
        db.close();
        return deletedRows;
    }
}

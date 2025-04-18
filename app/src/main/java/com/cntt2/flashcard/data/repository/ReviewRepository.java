package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.data.local.dao.ReviewDao;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.Review;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ReviewRepository {
    private static final String TAG = "ReviewRepository";
    private final ReviewDao reviewDao;
    private final IdMappingRepository idMappingRepository;
    private final SimpleDateFormat dateFormat;

    public ReviewRepository(Context context) {
        reviewDao = new ReviewDao(context);
        idMappingRepository = new IdMappingRepository(context);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public long insertReview(Review review) {
        Review existingReview = getReviewByCardId(review.getCardId());
        if (existingReview != null) {
            reviewDao.deleteReview(existingReview.getId());
            idMappingRepository.deleteIdMapping(existingReview.getId(), "review");
            Log.d(TAG, "Deleted existing review for cardId: " + review.getCardId() +
                    " before inserting new review");
        }

        String currentTime = dateFormat.format(new Date());
        if (review.getServerId() == null) {
            review.setSyncStatus("pending_create");
        } else {
            review.setSyncStatus("synced");
        }

        long localId = reviewDao.insertReview(review);
        if (review.getServerId() != null) {
            idMappingRepository.insertIdMappingSafe(new IdMapping((int) localId, review.getServerId(), "review"));
        }
        Log.d(TAG, "Inserted review - localId: " + localId +
                ", cardId: " + review.getCardId() +
                ", syncStatus: " + review.getSyncStatus());
        return localId;
    }

    public void updateReview(Review review, boolean fromSync) {
        Review existingReview = getReviewById(review.getId());
        if (existingReview != null) {
            if (!fromSync) {
                review.setLastModified(dateFormat.format(new Date()));
                review.setSyncStatus("pending_update");
            }
            reviewDao.updateReview(review);
            Log.d(TAG, "Updated review - ID: " + review.getId() + ", syncStatus: " + review.getSyncStatus() + ", fromSync: " + fromSync);
        } else {
            Log.w(TAG, "No existing review found to update - ID: " + review.getId());
        }
    }

    public void deleteReview(int reviewId) {
        Review review = getReviewById(reviewId);
        if (review != null) {
            review.setSyncStatus("pending_delete");
            updateReview(review, false);
            Log.d(TAG, "Marked review for deletion - ID: " + reviewId);
        } else {
            Log.w(TAG, "No review found to mark for deletion - ID: " + reviewId);
        }
    }

    public void deleteReviewConfirmed(int reviewId) {
        try {
            int rowsAffected = reviewDao.deleteReview(reviewId);
            idMappingRepository.deleteIdMapping(reviewId, "review");
            if (rowsAffected > 0) {
                Log.d(TAG, "Successfully deleted review - ID: " + reviewId);
            } else {
                Log.w(TAG, "No review found to delete - ID: " + reviewId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete review - ID: " + reviewId + ", error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to delete review: " + e.getMessage());
        }
    }

    public Review getReviewById(int id) {
        Review review = reviewDao.getReviewById(id);
        if (review == null) {
            Log.w(TAG, "No review found - ID: " + id);
        }
        return review;
    }

    public List<Card> getCardsToReview(int deskId) {
        List<Card> cards = reviewDao.getCardsToReview(deskId);
        cards.removeIf(card -> "pending_delete".equals(card.getSyncStatus()));
        Log.d(TAG, "Retrieved " + cards.size() + " cards to review for deskId: " + deskId);
        return new ArrayList<>(cards);
    }

    public List<Review> getAllReviews() {
        List<Review> reviews = reviewDao.getAllReviews();
        Log.d(TAG, "Retrieved " + reviews.size() + " reviews");
        return new ArrayList<>(reviews);
    }
    public List<Card> getNewCards(int deskId) {
        List<Card> cards = reviewDao.getNewCards(deskId);
        cards.removeIf(card -> "pending_delete".equals(card.getSyncStatus()));
        Log.d(TAG, "Retrieved " + cards.size() + " new cards for deskId: " + deskId);
        return new ArrayList<>(cards);
    }

    public Review getReviewByCardId(int cardId) {
        Review review = reviewDao.getReviewByCardId(cardId);
        if (review == null) {
            Log.w(TAG, "No review found for card - ID: " + cardId);
        }
        return review;
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        Review review = getReviewById(localId);
        if (review != null) {
            review.setSyncStatus(syncStatus);
            reviewDao.updateReview(review);
            Log.d(TAG, "Updated syncStatus for review - ID: " + localId + ", syncStatus: " + syncStatus);
        }
    }

    public List<Review> getPendingReviews(String syncStatus) {
        List<Review> reviews = reviewDao.getPendingReviews(syncStatus);
        Log.d(TAG, "Retrieved " + reviews.size() + " pending reviews with syncStatus: " + syncStatus);
        return reviews;
    }

    private boolean nullSafeEquals(Integer a, Integer b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
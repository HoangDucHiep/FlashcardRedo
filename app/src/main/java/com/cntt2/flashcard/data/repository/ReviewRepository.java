package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.data.local.dao.ReviewDao;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReviewRepository {
    private final ReviewDao reviewDao;
    private final IdMappingDao idMappingDao;

    public ReviewRepository(Context context) {
        reviewDao = new ReviewDao(context);
        idMappingDao = new IdMappingDao(context);
    }

    public void insertReview(Review review) {
        review.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        review.setSyncStatus("pending_create");
        reviewDao.insertReview(review);
    }

    public void updateReview(Review review) {
        review.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        review.setSyncStatus("pending_update");
        reviewDao.updateReview(review);
    }

    public List<Card> getCardsToReview(int deskId) {
        List<Card> cards = reviewDao.getCardsToReview(deskId);
        cards.removeIf(card -> "pending_delete".equals(card.getSyncStatus()));
        return cards;
    }

    public List<Card> getNewCards(int deskId) {
        List<Card> cards = reviewDao.getNewCards(deskId);
        cards.removeIf(card -> "pending_delete".equals(card.getSyncStatus()));
        return cards;
    }

    public Review getReviewByCardId(int cardId) {
        Review review = reviewDao.getReviewByCardId(cardId);
        if (review != null && "pending_delete".equals(review.getSyncStatus())) {
            return null;
        }
        return review;
    }

    public List<Review> getReviewsDueToday(int deskId, String today) {
        List<Review> reviews = reviewDao.getReviewsDueToday(deskId, today);
        reviews.removeIf(review -> "pending_delete".equals(review.getSyncStatus()));
        return reviews;
    }

    public void deleteReviewByCardId(int cardId) {
        Review review = getReviewByCardId(cardId);
        if (review != null) {
            review.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            review.setSyncStatus("pending_delete");
            reviewDao.updateReview(review);
        }
    }

    public void deleteReviewByCardIdConfirmed(int cardId) {
        reviewDao.deleteReviewByCardId(cardId);
    }

    public List<Review> getPendingReviews(String syncStatus) {
        return reviewDao.getPendingReviews(syncStatus);
    }

    public void insertIdMapping(long localId, String serverId) {
        reviewDao.insertIdMapping(localId, serverId, "review");
    }

    public Integer getLocalIdByServerId(String serverId) {
        return reviewDao.getLocalIdByServerId(serverId, "review");
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        reviewDao.updateSyncStatus(localId, syncStatus);
    }
}
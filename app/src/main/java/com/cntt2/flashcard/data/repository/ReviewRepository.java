package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.data.local.dao.ReviewDao;
import com.cntt2.flashcard.model.Review;

import java.util.List;

public class ReviewRepository {
    private ReviewDao reviewDao;

    public ReviewRepository(Context context) {
        reviewDao = new ReviewDao(context);
    }

    public void insertReview(Review review) {
        reviewDao.insertReview(review);
    }

    public void updateReview(Review review) {
        reviewDao.updateReview(review);
    }

    public List<Review> getReviewsDueToday(int deskId, String today) {
        return reviewDao.getReviewsDueToday(deskId, today);
    }
}

package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.CardDao;
import com.cntt2.flashcard.data.local.dao.ReviewDao;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CardRepository {
    private CardDao cardDao;
    private ReviewRepository reviewRepository = App.getInstance().getReviewRepository();

    public CardRepository(Context context) {
        cardDao = new CardDao(context);
    }

    public long insertCard(Card card) {
        long cardId = cardDao.insertCard(card);
        if (cardId != -1) {
            Review review = new Review();
            review.setCardId((int) cardId);
            review.setEase(2.5f);
            review.setInterval(0);
            review.setRepetition(0);
            review.setNextReviewDate(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            review.setLastReviewed(null);
            reviewRepository.insertReview(review);
        }
        return cardId;
    }

    public void updateCard(Card card) {
        cardDao.updateCard(card);
    }

    public void deleteCard(Card card) {
        cardDao.deleteCard(card.getId());
        reviewRepository.deleteReviewByCardId(card.getId());
    }

    public Card getCardById(int cardId) {
        return cardDao.getCardById(cardId);
    }

    public List<Card> getNewCards(int deskId) {
        return reviewRepository.getNewCards(deskId);
    }

    public List<Card> getCardsToReview(int deskId) {
        return reviewRepository.getCardsToReview(deskId);
    }


    public List<Card> getCardsByDeskId(int deskId) {
        return cardDao.getCardsByDeskId(deskId);
    }
}

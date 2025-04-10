package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.CardDao;
import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.data.local.dao.ReviewDao;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.Review;
import com.cntt2.flashcard.utils.ImageManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CardRepository {
    private CardDao cardDao;
    private ReviewRepository reviewRepository = App.getInstance().getReviewRepository();
    private Context context;
    private final IdMappingDao idMappingDao;

    public CardRepository(Context context) {
        this.context = context;
        cardDao = new CardDao(context);
        idMappingDao = new IdMappingDao(context);
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
            review.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            review.setSyncStatus("pending_create");
            reviewRepository.insertReview(review);
        }
        return cardId;
    }

    public void updateCard(Card card) {
        card.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        card.setSyncStatus("pending_update");
        cardDao.updateCard(card);
    }

    public void deleteCard(Card card) {
        card.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        card.setSyncStatus("pending_delete");
        cardDao.updateCard(card);
    }

    public void deleteCardConfirmed(int cardId) {
        Card card = cardDao.getCardById(cardId);
        String frontAndBackHtml = card != null ? card.getFront() + card.getBack() : "";
        int res = cardDao.deleteCard(cardId);
        if (res > 0) {
            reviewRepository.deleteReviewByCardId(cardId);
            var images = ImageManager.extractImagePathsFromHtml(frontAndBackHtml, context);
            ImageManager.deleteImageFiles(images, context);
        }
    }

    public Card getCardById(int cardId) {
        Card card = cardDao.getCardById(cardId);
        if (card != null && "pending_delete".equals(card.getSyncStatus())) {
            return null;
        }
        return card;
    }
    public List<Card> getNewCards(int deskId) {
        return reviewRepository.getNewCards(deskId);
    }

    public List<Card> getCardsToReview(int deskId) {
        return reviewRepository.getCardsToReview(deskId);
    }

    public List<Card> getCardsByDeskId(int deskId) {
        List<Card> cards = cardDao.getCardsByDeskId(deskId);
        cards.removeIf(card -> "pending_delete".equals(card.getSyncStatus())); // Lọc bỏ pending_delete
        return cards;
    }

    public List<Card> getPendingCards(String syncStatus) {
        return cardDao.getPendingCards(syncStatus);
    }

    public void insertIdMapping(long localId, String serverId) {
        cardDao.insertIdMapping(localId, serverId, "card");
    }

    public Integer getLocalIdByServerId(String serverId) {
        return cardDao.getLocalIdByServerId(serverId, "card");
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        cardDao.updateSyncStatus(localId, syncStatus);
    }
}

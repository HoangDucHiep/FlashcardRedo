package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.CardDao;
import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.data.local.dao.ReviewDao;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.Review;
import com.cntt2.flashcard.utils.ImageManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CardRepository {
    private static final String TAG = "CardRepository";
    private CardDao cardDao;
    private ReviewRepository reviewRepository = App.getInstance().getReviewRepository();
    private Context context;
    private final IdMappingRepository idMappingRepository;
    private final SimpleDateFormat dateFormat;

    public CardRepository(Context context) {
        this.context = context;
        cardDao = new CardDao(context);
        idMappingRepository = new IdMappingRepository(context);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public long insertCard(Card card) {
        String currentTime = dateFormat.format(new Date());
        card.setLastModified(currentTime);
        if (card.getCreatedAt() == null) {
            card.setCreatedAt(currentTime);
        }
        if (card.getServerId() == null) {
            card.setSyncStatus("pending_create");
        } else {
            card.setSyncStatus("synced");
        }

        long cardId = cardDao.insertCard(card);
        if (cardId != -1) {
            if (card.getServerId() != null) {
                idMappingRepository.insertIdMapping(new IdMapping((int) cardId, card.getServerId(), "card"));
            }
            Review review = new Review();
            review.setCardId((int) cardId);
            review.setEase(2.5);
            review.setInterval(0);
            review.setRepetition(0);
            review.setNextReviewDate(null);
            review.setLastReviewed(null);
            review.setSyncStatus("pending_create");
            long reviewId = reviewRepository.insertReview(review);
            if (reviewId == -1) {
                Log.e(TAG, "Failed to insert review for cardId: " + cardId);
            }
        }
        return cardId;
    }

    public void updateCard(Card card, boolean fromSync) {
        Card existingCard = cardDao.getCardById(card.getId());
        if (existingCard != null) {
            if (!fromSync && (!existingCard.getFront().equals(card.getFront()) ||
                    !existingCard.getBack().equals(card.getBack()) ||
                    !nullSafeEquals(existingCard.getDeskId(), card.getDeskId()))) {
                card.setLastModified(dateFormat.format(new Date()));
                card.setSyncStatus("pending_update");
            }
            cardDao.updateCard(card);
        }
    }

    public void deleteCard(Card card) {
        if (card == null) {
            Log.w(TAG, "Attempted to delete null card");
            return;
        }
        card.setSyncStatus("pending_delete");
        updateCard(card, false);
        Log.d(TAG, "Marked card for deletion - ID: " + card.getId());
        Review review = reviewRepository.getReviewByCardId(card.getId());
        if (review != null && review.getCardId() == card.getId()) {
            reviewRepository.deleteReview(review.getId());
        }
    }

    public void deleteCardConfirmed(int cardId) {
        try {
            int rowsAffected = cardDao.deleteCard(cardId);
            if (rowsAffected > 0) {
                Card card = cardDao.getCardById(cardId); // Lấy card trước khi xóa (nếu cần log chi tiết)
                idMappingRepository.deleteIdMapping(cardId, "card");
                if (card != null) {
                    String frontAndBackHtml = card.getFront() + card.getBack();
                    var images = ImageManager.extractImagePathsFromHtml(frontAndBackHtml, context);
                    ImageManager.deleteImageFiles(images, context);
                }
                Log.d(TAG, "Successfully deleted card and mapping - ID: " + cardId);
            } else {
                Log.w(TAG, "No card found to delete - ID: " + cardId);
                idMappingRepository.deleteIdMapping(cardId, "card"); // Xóa ánh xạ để tránh rò rỉ
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete card - ID: " + cardId + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to delete card: " + e.getMessage());
        }
    }

    public Card getCardById(int cardId) {
        Card card = cardDao.getCardById(cardId);
        if (card == null) {
            Log.w(TAG, "No card found - ID: " + cardId);
        }
        return card; // Sửa lỗi, trả về card thay vì null
    }

    public List<Card> getNewCards(int deskId) {
        return reviewRepository.getNewCards(deskId);
    }

    public List<Card> getCardsToReview(int deskId) {
        return reviewRepository.getCardsToReview(deskId);
    }

    public List<Card> getCardsByDeskId(int deskId) {
        List<Card> cards = cardDao.getCardsByDeskId(deskId);
        cards.removeIf(card -> "pending_delete".equals(card.getSyncStatus()));
        return new ArrayList<>(cards);
    }

    public List<Card> getPendingCards(String syncStatus) {
        List<Card> allCards = cardDao.getAllCards();
        List<Card> pendingCards = new ArrayList<>();
        for (Card card : allCards) {
            if (syncStatus.equals(card.getSyncStatus())) {
                pendingCards.add(card);
            }
        }
        Log.d(TAG, "getPendingCards(" + syncStatus + ") returned " + pendingCards.size() + " cards");
        return pendingCards;
    }

    public void updateSyncStatus(int cardId, String syncStatus) {
        Card card = getCardById(cardId);
        if (card != null) {
            card.setSyncStatus(syncStatus);
            cardDao.updateCard(card);
        }
    }

    private boolean nullSafeEquals(Integer a, Integer b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
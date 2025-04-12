package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.CardDao;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.Review;
import com.cntt2.flashcard.utils.ImageManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class CardRepository {
    private static final String TAG = "CardRepository";
    private final CardDao cardDao;
    private final ReviewRepository reviewRepository = App.getInstance().getReviewRepository();
    private final Context context;
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
        return insertCard(card, false); // Gọi từ UI, không phải đồng bộ
    }

    public long insertCard(Card card, boolean fromSync) {
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
            Log.d(TAG, "Inserted card - ID: " + cardId + ", syncStatus: " + card.getSyncStatus());
            if (card.getServerId() != null) {
                idMappingRepository.insertIdMapping(new IdMapping((int) cardId, card.getServerId(), "card"));
            }
            // Chỉ tạo Review mặc định nếu không phải từ đồng bộ
            if (!fromSync) {
                Review review = new Review();
                review.setCardId((int) cardId);
                review.setEase(2.5);
                review.setInterval(0);
                review.setRepetition(0);
                review.setNextReviewDate(currentTime);
                review.setLastReviewed(null);
                review.setSyncStatus("pending_create");
                long reviewId = reviewRepository.insertReview(review);
                if (reviewId == -1) {
                    Log.e(TAG, "Failed to insert review for cardId: " + cardId);
                    cardDao.deleteCard((int) cardId);
                    return -1;
                }
            }
        } else {
            Log.e(TAG, "Failed to insert card");
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
            Log.d(TAG, "Updated card - ID: " + card.getId() + ", syncStatus: " + card.getSyncStatus() + ", fromSync: " + fromSync);
        } else {
            Log.w(TAG, "No existing card found to update - ID: " + card.getId());
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

        // Xóa tất cả Review liên quan
        Review review = reviewRepository.getReviewByCardId(card.getId());
        if (review != null) {
            reviewRepository.deleteReview(review.getId());
            Log.d(TAG, "Marked review for deletion - Review ID: " + review.getId() + " for Card ID: " + card.getId());
        } else {
            Log.w(TAG, "No review found for card - ID: " + card.getId());
        }
    }

    public void deleteCardConfirmed(int cardId) {
        try {
            // Lấy Card trước khi xóa
            Card card = cardDao.getCardById(cardId);

            // Xóa Card khỏi cơ sở dữ liệu
            int rowsAffected = cardDao.deleteCard(cardId);
            if (rowsAffected > 0) {
                // Xóa IdMapping
                idMappingRepository.deleteIdMapping(cardId, "card");

                // Xử lý xóa ảnh nếu Card tồn tại
                if (card != null) {
                    String frontAndBackHtml = card.getFront() + card.getBack();
                    Set<String> images = ImageManager.extractImagePathsFromHtml(frontAndBackHtml, context);
                    ImageManager.deleteImageFiles(images, context);
                    Log.d(TAG, "Successfully deleted card, mapping, and associated images - ID: " + cardId);
                } else {
                    Log.w(TAG, "Card was deleted but not found in query before deletion - ID: " + cardId);
                }
            } else {
                // Không tìm thấy Card, vẫn thử xóa IdMapping để đảm bảo sạch dữ liệu
                idMappingRepository.deleteIdMapping(cardId, "card");
                Log.w(TAG, "No card found to delete - ID: " + cardId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete card - ID: " + cardId + ", error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to delete card: " + e.getMessage());
        }
    }

    public Card getCardById(int cardId) {
        Card card = cardDao.getCardById(cardId);
        if (card == null) {
            Log.w(TAG, "No card found - ID: " + cardId);
        }
        return card;
    }

    public List<Card> getNewCards(int deskId) {
        List<Card> cards = reviewRepository.getNewCards(deskId);
        Log.d(TAG, "Retrieved " + cards.size() + " new cards for deskId: " + deskId);
        return new ArrayList<>(cards);
    }

    public List<Card> getAllCards()
    {
        List<Card> allCards = cardDao.getAllCards();
        return new ArrayList<>(allCards);
    }

    public List<Card> getCardsToReview(int deskId) {
        List<Card> cards = reviewRepository.getCardsToReview(deskId);
        Log.d(TAG, "Retrieved " + cards.size() + " cards to review for deskId: " + deskId);
        return new ArrayList<>(cards);
    }

    public List<Card> getCardsByDeskId(int deskId) {
        List<Card> cards = cardDao.getCardsByDeskId(deskId);
        cards.removeIf(card -> "pending_delete".equals(card.getSyncStatus()));
        Log.d(TAG, "Retrieved " + cards.size() + " cards for deskId: " + deskId);
        return new ArrayList<>(cards);
    }

    public List<Card> getPendingCards(String syncStatus) {
        List<Card> pendingCards = cardDao.getPendingCards(syncStatus);
        Log.d(TAG, "getPendingCards(" + syncStatus + ") returned " + pendingCards.size() + " cards");
        return pendingCards;
    }

    public void updateSyncStatus(int cardId, String syncStatus) {
        Card card = getCardById(cardId);
        if (card != null) {
            card.setSyncStatus(syncStatus);
            cardDao.updateCard(card);
            Log.d(TAG, "Updated syncStatus for card - ID: " + cardId + ", syncStatus: " + syncStatus);
        }
    }

    private boolean nullSafeEquals(Integer a, Integer b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
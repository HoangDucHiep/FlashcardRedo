package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.data.local.dao.CardDao;
import com.cntt2.flashcard.model.Card;

import java.util.List;

public class CardRepository {
    private CardDao cardDao;

    public CardRepository(Context context) {
        cardDao = new CardDao(context);
    }

    public void insertCard(Card card) {
        cardDao.insertCard(card);
    }

    public void deleteCard(Card card) {
        cardDao.deleteCard(card.getId());
    }

    public List<Card> getCardsByDeskId(int deskId) {
        return cardDao.getCardsByDeskId(deskId);
    }
}

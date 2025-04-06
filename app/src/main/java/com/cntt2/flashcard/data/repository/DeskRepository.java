package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.data.local.dao.CardDao;
import com.cntt2.flashcard.data.local.dao.DeskDao;
import com.cntt2.flashcard.model.Desk;

import java.util.List;

public class DeskRepository {
    private DeskDao deskDao;
    private CardDao cardDao;

    public DeskRepository(Context context) {
        this.deskDao = new DeskDao(context);
        this.cardDao = new CardDao(context);
    }

    public List<Desk> getAllDesks() {
        return deskDao.getAllDesks();
    }

    public long insertDesk(Desk desk) {
        return deskDao.insertDesk(desk);
    }

    public void deleteDesk(Desk desk) {
        // delete all cards in the desk

        var cards = desk.getCards();

        for (var card : cards) {
            cardDao.deleteCard(card.getId());
        }

        deskDao.deleteDesk(desk.getId());
    }

    public List<Desk> getDesksByFolderId(int folderId) {
        return deskDao.getDesksByFolderId(folderId);
    }

    public void updateDesk(Desk desk) {
        deskDao.updateDesk(desk);
    }
}

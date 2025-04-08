package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.CardDao;
import com.cntt2.flashcard.data.local.dao.DeskDao;
import com.cntt2.flashcard.model.Desk;

import java.util.List;

public class DeskRepository {
    private DeskDao deskDao;
    private CardRepository cardRepository = App.getInstance().getCardRepository();;
    private LearningSessionRepository learningSessionRepository = App.getInstance().getLearningSessionRepository();

    public DeskRepository(Context context) {
        this.deskDao = new DeskDao(context);
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
            cardRepository.deleteCard(card);
        }

        var sessions = learningSessionRepository.getSessionsByDeskId(desk.getId());
        for (var session : sessions) {
            learningSessionRepository.deleteSession(session.getId());
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

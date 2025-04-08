package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.data.local.dao.LearningSessionDao;
import com.cntt2.flashcard.model.LearningSession;

import java.util.List;

public class LearningSessionRepository {
    private LearningSessionDao sessionDao;

    public LearningSessionRepository(Context context) {
        sessionDao = new LearningSessionDao(context);
    }

    public long insertSession(LearningSession session) {
        return sessionDao.insertSession(session);
    }

    public List<LearningSession> getSessionsByDeskId(int deskId) {
        return sessionDao.getSessionsByDeskId(deskId);
    }
}

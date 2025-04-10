package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.data.local.dao.LearningSessionDao;
import com.cntt2.flashcard.model.LearningSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LearningSessionRepository {
    private final LearningSessionDao sessionDao;
    private final IdMappingDao idMappingDao;

    public LearningSessionRepository(Context context) {
        sessionDao = new LearningSessionDao(context);
        idMappingDao = new IdMappingDao(context);
    }

    public long insertSession(LearningSession session) {
        session.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        session.setSyncStatus("pending_create");
        return sessionDao.insertLearningSession(session);
    }

    public List<LearningSession> getSessionsByDeskId(int deskId) {
        List<LearningSession> sessions = sessionDao.getSessionsByDeskId(deskId);
        sessions.removeIf(session -> "pending_delete".equals(session.getSyncStatus()));
        return sessions;
    }

    public void deleteSession(int sessionId) {
        LearningSession session = new LearningSession();
        session.setId(sessionId);
        session.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        session.setSyncStatus("pending_delete");
        sessionDao.updateLearningSession(session);
    }

    public void deleteSessionConfirmed(int sessionId) {
        sessionDao.deleteLearningSession(sessionId);
    }

    public List<LearningSession> getPendingSessions(String syncStatus) {
        return sessionDao.getPendingSessions(syncStatus); // Không lọc ở đây vì đây là lấy bản ghi pending
    }

    public void insertIdMapping(long localId, String serverId) {
        sessionDao.insertIdMapping(localId, serverId, "session");
    }

    public Integer getLocalIdByServerId(String serverId) {
        return sessionDao.getLocalIdByServerId(serverId, "session");
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        sessionDao.updateSyncStatus(localId, syncStatus);
    }
}
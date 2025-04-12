package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.data.local.dao.LearningSessionDao;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.LearningSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class LearningSessionRepository {
    private static final String TAG = "LearningSessionRepository";
    private final LearningSessionDao sessionDao;
    private final IdMappingRepository idMappingRepository;
    private final SimpleDateFormat dateFormat;

    public LearningSessionRepository(Context context) {
        sessionDao = new LearningSessionDao(context);
        idMappingRepository = new IdMappingRepository(context);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public long insertSession(LearningSession session) {
        String currentTime = dateFormat.format(new Date());
        session.setLastModified(currentTime);
        if (session.getServerId() == null) {
            session.setSyncStatus("pending_create");
        } else {
            session.setSyncStatus("synced");
        }
        long localId = sessionDao.insertLearningSession(session);
        if (session.getServerId() != null) {
            idMappingRepository.insertIdMapping(new IdMapping((int) localId, session.getServerId(), "session"));
        }
        return localId;
    }

    public void updateSession(LearningSession session, boolean fromSync) {
        LearningSession existingSession = getSessionById(session.getId());
        if (existingSession != null) {
            if (!fromSync) {
                session.setLastModified(dateFormat.format(new Date()));
                session.setSyncStatus("pending_update");
            }
            sessionDao.updateLearningSession(session);
        }
    }

    public void deleteSession(int sessionId) {
        LearningSession session = getSessionById(sessionId);
        if (session != null) {
            session.setSyncStatus("pending_delete");
            updateSession(session, false);
            Log.d(TAG, "Marked session for deletion - ID: " + sessionId);
        }
    }

    public void deleteSessionConfirmed(int sessionId) {
        try {
            int rowsAffected = sessionDao.deleteLearningSession(sessionId);
            if (rowsAffected > 0) {
                idMappingRepository.deleteIdMapping(sessionId, "session");
                Log.d(TAG, "Successfully deleted session and mapping - ID: " + sessionId);
            } else {
                Log.w(TAG, "No session found to delete - ID: " + sessionId);
                idMappingRepository.deleteIdMapping(sessionId, "session");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete session - ID: " + sessionId + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to delete session: " + e.getMessage());
        }
    }

    public LearningSession getSessionById(int id) {
        LearningSession session = sessionDao.getSessionById(id);
        if (session == null) {
            Log.w(TAG, "No session found - ID: " + id);
        }
        return session;
    }

    public List<LearningSession> getSessionsByDeskId(int deskId) {
        List<LearningSession> sessions = sessionDao.getSessionsByDeskId(deskId);
        sessions.removeIf(session -> "pending_delete".equals(session.getSyncStatus()));
        return new ArrayList<>(sessions);
    }

    public List<LearningSession> getPendingSessions(String syncStatus) {
        return sessionDao.getPendingSessions(syncStatus);
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        LearningSession session = sessionDao.getSessionById(localId);
        if (session != null) {
            session.setSyncStatus(syncStatus);
            sessionDao.updateLearningSession(session);
        }
    }
}
package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.data.local.dao.LearningSessionDao;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.LearningSession;

import java.text.SimpleDateFormat;
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
        validateSession(session);
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
        Log.d(TAG, "Inserted session with localId: " + localId);
        return localId;
    }

    public boolean updateSession(LearningSession session, boolean fromSync) {
        validateSession(session);
        LearningSession existingSession = getSessionById(session.getId());
        if (existingSession == null) {
            Log.w(TAG, "Session not found for update - ID: " + session.getId());
            return false;
        }

        if (!fromSync && (!existingSession.getStartTime().equals(session.getStartTime()) ||
                !nullSafeEquals(existingSession.getEndTime(), session.getEndTime()) ||
                existingSession.getCardsStudied() != session.getCardsStudied() ||
                existingSession.getPerformance() != session.getPerformance())) {
            session.setLastModified(dateFormat.format(new Date()));
            session.setSyncStatus("pending_update");
        }
        sessionDao.updateLearningSession(session);
        Log.d(TAG, "Updated session with localId: " + session.getId());
        return true;
    }

    public boolean deleteSession(int sessionId) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            Log.w(TAG, "Session not found for deletion - ID: " + sessionId);
            return false;
        }
        session.setSyncStatus("pending_delete");
        session.setLastModified(dateFormat.format(new Date()));
        sessionDao.updateLearningSession(session);
        Log.d(TAG, "Marked session for deletion - ID: " + sessionId);
        return true;
    }

    public boolean deleteSessionConfirmed(int sessionId) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            Log.w(TAG, "Session not found for confirmed deletion - ID: " + sessionId);
            return false;
        }
        int rowsAffected = sessionDao.deleteLearningSession(sessionId);
        if (rowsAffected > 0) {
            idMappingRepository.deleteIdMapping(sessionId, "session");
            Log.d(TAG, "Confirmed deletion of session - ID: " + sessionId);
            return true;
        }
        Log.w(TAG, "Failed to delete session - ID: " + sessionId);
        return false;
    }

    public LearningSession getSessionById(int id) {
        return sessionDao.getSessionById(id);
    }

    public List<LearningSession> getSessionsByDeskId(int deskId) {
        List<LearningSession> sessions = sessionDao.getSessionsByDeskId(deskId);
        sessions.removeIf(session -> "pending_delete".equals(session.getSyncStatus()));
        return sessions;
    }

    public List<LearningSession> getPendingSessions(String syncStatus) {
        return sessionDao.getPendingSessions(syncStatus);
    }

    public boolean updateSyncStatus(int localId, String syncStatus) {
        LearningSession session = getSessionById(localId);
        if (session == null) {
            Log.w(TAG, "Session not found for sync status update - ID: " + localId);
            return false;
        }
        sessionDao.updateSyncStatus(localId, syncStatus);
        Log.d(TAG, "Updated sync status for session - ID: " + localId + ", Status: " + syncStatus);
        return true;
    }

    private void validateSession(LearningSession session) {
        if (session.getDeskId() <= 0) {
            throw new IllegalArgumentException("Invalid deskId: " + session.getDeskId());
        }
        if (session.getStartTime() == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        try {
            dateFormat.parse(session.getStartTime());
            if (session.getEndTime() != null) {
                dateFormat.parse(session.getEndTime());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + e.getMessage());
        }
        if (session.getCardsStudied() < 0) {
            throw new IllegalArgumentException("Cards studied cannot be negative");
        }
        if (session.getPerformance() < 0 || session.getPerformance() > 100) {
            throw new IllegalArgumentException("Performance must be between 0 and 100");
        }
    }

    private boolean nullSafeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
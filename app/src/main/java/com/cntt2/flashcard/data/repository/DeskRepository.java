package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.DeskDao;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.IdMapping;
import com.cntt2.flashcard.model.LearningSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DeskRepository {
    private static final String TAG = "DeskRepository";
    private final DeskDao deskDao;
    private final CardRepository cardRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final IdMappingRepository idMappingRepository;
    private final SimpleDateFormat dateFormat;

    public DeskRepository(Context context) {
        this.deskDao = new DeskDao(context);
        this.cardRepository = App.getInstance().getCardRepository();
        this.learningSessionRepository = App.getInstance().getLearningSessionRepository();
        idMappingRepository = new IdMappingRepository(context);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public long insertDesk(Desk desk) {
        String currentTime = dateFormat.format(new Date());
        desk.setLastModified(currentTime);
        if (desk.getCreatedAt() == null) {
            desk.setCreatedAt(currentTime);
        }
        if (desk.getServerId() == null) {
            desk.setSyncStatus("pending_create");
        } else {
            desk.setSyncStatus("synced");
        }

        long localId = deskDao.insertDesk(desk);
        return localId;
    }

    public void updateDesk(Desk desk, boolean fromSync) {
        Desk existingDesk = getDeskById(desk.getId());
        if (existingDesk != null) {
            if (!fromSync && (!existingDesk.getName().equals(desk.getName()) ||
                    !nullSafeEquals(existingDesk.getFolderId(), desk.getFolderId()))) {
                desk.setLastModified(dateFormat.format(new Date()));
                desk.setSyncStatus("pending_update");
            }
            deskDao.updateDesk(desk); // Luôn cập nhật vào database
        }
    }

    public void deleteDesk(Desk desk) {
        if (desk == null) {
            Log.w(TAG, "Attempted to delete null desk");
            return;
        }
        desk.setSyncStatus("pending_delete");
        deskDao.updateDesk(desk);
        Log.d(TAG, "Marked desk for deletion - ID: " + desk.getId());
        List<Card> cards = cardRepository.getCardsByDeskId(desk.getId());
        String serverId = idMappingRepository.getServerIdByLocalId(desk.getId(), "desk");
        for (Card card : cards) {
            if (serverId == null) {
                cardRepository.deleteCardConfirmed(card.getId());
            } else {
                cardRepository.deleteCard(card);
            }
        }
        List<LearningSession> sessions = learningSessionRepository.getSessionsByDeskId(desk.getId());
        for (LearningSession session : sessions) {
            learningSessionRepository.deleteSession(session.getId());
        }
    }

    public void deleteDeskConfirmed(int deskId) {
        Desk desk = getDeskById(deskId);
        if (desk != null) {
            deskDao.deleteDesk(deskId);
            idMappingRepository.deleteIdMapping(deskId, "desk");
            Log.d(TAG, "Deleted desk with localId: " + deskId);
        }
    }

    public Desk getDeskById(int deskId) {
        Desk desk = deskDao.getDeskById(deskId);
        if (desk == null) {
            Log.w(TAG, "No desk found - ID: " + deskId);
        }
        return desk;
    }

    public List<Desk> getAllDesks() {
        List<Desk> desks = deskDao.getAllDesks();
        desks.removeIf(desk -> "pending_delete".equals(desk.getSyncStatus()));
        return new ArrayList<>(desks);
    }

    public List<Desk> getDesksByFolderId(int folderId) {
        List<Desk> allDesks = getAllDesks();
        List<Desk> desks = new ArrayList<>();
        for (Desk desk : allDesks) {
            if (desk.getFolderId() != null && desk.getFolderId() == folderId) {
                desks.add(desk);
            }
        }
        return desks;
    }

    public List<Desk> getPendingDesks(String syncStatus) {
        List<Desk> allDesks = deskDao.getAllDesks();
        List<Desk> pendingDesks = new ArrayList<>();
        for (Desk desk : allDesks) {
            if (syncStatus.equals(desk.getSyncStatus())) {
                pendingDesks.add(desk);
            }
        }
        return pendingDesks;
    }

    public void updateSyncStatus(int deskId, String syncStatus) {
        Desk desk = getDeskById(deskId);
        if (desk != null) {
            desk.setSyncStatus(syncStatus);
            deskDao.updateDesk(desk);
        }
    }

    private boolean nullSafeEquals(Integer a, Integer b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
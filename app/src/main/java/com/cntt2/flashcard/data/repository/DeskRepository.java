package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.local.dao.DeskDao;
import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.model.Desk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DeskRepository {
    private final DeskDao deskDao;
    private final CardRepository cardRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final IdMappingDao idMappingDao;

    public DeskRepository(Context context) {
        this.deskDao = new DeskDao(context);
        this.cardRepository = App.getInstance().getCardRepository();
        this.learningSessionRepository = App.getInstance().getLearningSessionRepository();
        this.idMappingDao = new IdMappingDao(context);
    }

    public List<Desk> getAllDesks() {
        List<Desk> desks = deskDao.getAllDesks();
        desks.removeIf(desk -> "pending_delete".equals(desk.getSyncStatus()));
        return desks;
    }

    public long insertDesk(Desk desk) {
        desk.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        desk.setSyncStatus("pending_create");
        return deskDao.insertDesk(desk);
    }

    public void deleteDesk(Desk desk) {
        var cards = desk.getCards();
        for (var card : cards) {
            cardRepository.deleteCard(card);
        }

        var sessions = learningSessionRepository.getSessionsByDeskId(desk.getId());
        for (var session : sessions) {
            learningSessionRepository.deleteSession(session.getId());
        }

        desk.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        desk.setSyncStatus("pending_delete");
        deskDao.updateDesk(desk);
    }

    public void deleteDeskConfirmed(int deskId) {
        deskDao.deleteDesk(deskId);
    }

    public Desk getDeskById(int deskId) {
        Desk desk = deskDao.getDeskById(deskId);
        if (desk != null && "pending_delete".equals(desk.getSyncStatus())) {
            return null;
        }
        return desk;
    }

    public List<Desk> getDesksByFolderId(int folderId) {
        List<Desk> desks = deskDao.getDesksByFolderId(folderId);
        desks.removeIf(desk -> "pending_delete".equals(desk.getSyncStatus()));
        return desks;
    }

    public void updateDesk(Desk desk) {
        desk.setLastModified(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        desk.setSyncStatus("pending_update");
        deskDao.updateDesk(desk);
    }

    public List<Desk> getPendingDesks(String syncStatus) {
        return deskDao.getPendingDesks(syncStatus); // Không lọc ở đây vì đây là lấy bản ghi pending
    }

    public void insertIdMapping(long localId, String serverId) {
        deskDao.insertIdMapping(localId, serverId, "desk");
    }

    public Integer getLocalIdByServerId(String serverId) {
        return deskDao.getLocalIdByServerId(serverId, "desk");
    }

    public void updateSyncStatus(int localId, String syncStatus) {
        deskDao.updateSyncStatus(localId, syncStatus);
    }
}
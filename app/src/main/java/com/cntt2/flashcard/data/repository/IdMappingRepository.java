package com.cntt2.flashcard.data.repository;

import android.content.Context;

import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.model.IdMapping;

import java.util.List;

public class IdMappingRepository {
    private final IdMappingDao idMappingDao;

    public IdMappingRepository(Context context) {
        this.idMappingDao = new IdMappingDao(context);
    }

    public long insertIdMapping(IdMapping mapping) {
        return idMappingDao.insertIdMapping(mapping);
    }

    public void updateIdMapping(IdMapping mapping) {
        idMappingDao.updateIdMapping(mapping);
    }

    public int deleteIdMapping(int localId, String entityType) {
        return idMappingDao.deleteIdMapping(localId, entityType);
    }

    public List<IdMapping> getAllMappings() {
        return idMappingDao.getAllMappings();
    }

    public Integer getLocalIdByServerId(String serverId, String entityType) {
        return idMappingDao.getLocalIdByServerId(serverId, entityType);
    }

    public String getServerIdByLocalId(int localId, String entityType) {
        return idMappingDao.getServerIdByLocalId(localId, entityType);
    }

    public List<IdMapping> getMappingsByEntityType(String entityType) {
        return idMappingDao.getMappingsByEntityType(entityType);
    }
}
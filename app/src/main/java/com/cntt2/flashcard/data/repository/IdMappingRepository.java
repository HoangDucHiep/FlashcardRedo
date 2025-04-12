package com.cntt2.flashcard.data.repository;

import android.content.Context;
import android.util.Log;

import com.cntt2.flashcard.data.local.dao.IdMappingDao;
import com.cntt2.flashcard.model.IdMapping;

public class IdMappingRepository {
    private static final String TAG = "IdMappingRepository";
    private final IdMappingDao idMappingDao;

    public IdMappingRepository(Context context) {
        idMappingDao = new IdMappingDao(context);
    }

    public void insertIdMapping(IdMapping idMapping) {
        idMappingDao.insertIdMapping(idMapping);
        Log.d(TAG, "Inserted id_mapping - localId: " + idMapping.getLocalId() +
                ", serverId: " + idMapping.getServerId() +
                ", entityType: " + idMapping.getEntityType());
    }

    public void insertIdMappingSafe(IdMapping idMapping) {
        Integer existingLocalId = getLocalIdByServerId(idMapping.getServerId(), idMapping.getEntityType());
        if (existingLocalId != null) {
            Log.w(TAG, "Id mapping already exists for serverId: " + idMapping.getServerId() +
                    ", entityType: " + idMapping.getEntityType() +
                    ", existing localId: " + existingLocalId);
            return;
        }
        try {
            idMappingDao.insertIdMapping(idMapping);
            Log.d(TAG, "Inserted id_mapping - localId: " + idMapping.getLocalId() +
                    ", serverId: " + idMapping.getServerId() +
                    ", entityType: " + idMapping.getEntityType());
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert id_mapping for localId: " + idMapping.getLocalId() +
                    ", serverId: " + idMapping.getServerId() +
                    ", entityType: " + idMapping.getEntityType() + ": " + e.getMessage());
        }
    }

    public void deleteIdMapping(int localId, String entityType) {
        idMappingDao.deleteIdMapping(localId, entityType);
        Log.d(TAG, "Deleted id_mapping - localId: " + localId +
                ", entityType: " + entityType);
    }

    public String getServerIdByLocalId(int localId, String entityType) {
        return idMappingDao.getServerIdByLocalId(localId, entityType);
    }

    public Integer getLocalIdByServerId(String serverId, String entityType) {
        return idMappingDao.getLocalIdByServerId(serverId, entityType);
    }
}
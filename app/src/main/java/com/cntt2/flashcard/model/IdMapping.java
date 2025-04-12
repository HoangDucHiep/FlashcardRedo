package com.cntt2.flashcard.model;

public class IdMapping {
    private int localId;      // ID local (integer từ SQLite)
    private String serverId;  // ID server (GUID từ API)
    private String entityType; // Loại thực thể ('folder', 'desk', 'card', 'review', 'session')

    public IdMapping() {}

    public IdMapping(int localId, String serverId, String entityType) {
        this.localId = localId;
        this.serverId = serverId;
        this.entityType = entityType;
    }

    // Getters và Setters
    public int getLocalId() { return localId; }
    public void setLocalId(int localId) { this.localId = localId; }
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
}
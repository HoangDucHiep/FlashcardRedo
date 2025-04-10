package com.cntt2.flashcard.model;

public class Card {
    private int id;
    private String serverId;
    private int deskId;
    private String front;
    private String back;
    private String createdAt;
    private String lastModified;
    private String syncStatus;

    public Card() {}

    public Card(String front, String back, String createdAt) {
        this.front = front;
        this.back = back;
        this.createdAt = createdAt;
    }

    public Card(String front, String back, int deskId, String createdAt) {
        this.front = front;
        this.back = back;
        this.createdAt = createdAt;
        this.deskId = deskId;
    }

    public Card(String front, String back, int deskId, String createdAt, String lastModified, String syncStatus) {
        this.front = front;
        this.back = back;
        this.deskId = deskId;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.syncStatus = syncStatus;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDeskId() {
        return deskId;
    }

    public void setDeskId(int deskId) {
        this.deskId = deskId;
    }

    public String getFront() {
        return front;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public String getBack() {
        return back;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

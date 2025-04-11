package com.cntt2.flashcard.model;

import java.util.ArrayList;
import java.util.List;

public class Desk {
    private int id;
    private String serverId;
    private Integer folderId;
    private String name;
    private boolean isPublic = false;
    private String createdAt;
    private String lastModified;
    private String syncStatus;

    private List<Card> cards = new ArrayList<>();



    public Desk(String name, Integer folderId, boolean isPublic, String createdAt, String lastModified, String syncStatus) {
        this.name = name;
        this.folderId = folderId;
        this.isPublic = isPublic;
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

    public Desk() {}

    public Desk(String name, int folderId, String createdAt) {
        this.name = name;
        this.folderId = folderId;
        this.createdAt = createdAt;
    }


    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public void addCard(Card card) {
        this.cards.add(card);
    }
}
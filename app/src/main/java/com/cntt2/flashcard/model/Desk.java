package com.cntt2.flashcard.model;

import java.util.ArrayList;
import java.util.List;

public class Desk {
    private int id;
    private Integer folderId;
    private String name;

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    private boolean isPublic = false;
    private String createdAt;
    private List<Card> cards = new ArrayList<>();

    public Desk() {}

    public Desk(String name, int folderId, String createdAt) {
        this.name = name;
        this.folderId = folderId;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFolderId() {
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
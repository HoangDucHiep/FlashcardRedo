package com.cntt2.flashcard.model;

public class Card {
    private int id;
    private int deskId;
    private String front;
    private String back;
    private String createdAt;

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

package com.cntt2.flashcard.model;

public class Review {
    private int id;
    private String serverId;
    private int cardId;
    private double ease;
    private int interval;
    private int repetition;
    private String nextReviewDate;
    private String lastReviewed;
    private String lastModified;
    private String syncStatus;

    public Review() {}

    public Review(int cardId, double ease, int interval, int repetition, String nextReviewDate) {
        this.cardId = cardId;
        this.ease = ease;
        this.interval = interval;
        this.repetition = repetition;
        this.nextReviewDate = nextReviewDate;
    }

    public Review(int cardId, double ease, int interval, int repetition, String nextReviewDate,
                  String lastReviewed, String lastModified, String syncStatus) {
        this.cardId = cardId;
        this.ease = ease;
        this.interval = interval;
        this.repetition = repetition;
        this.nextReviewDate = nextReviewDate;
        this.lastReviewed = lastReviewed;
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

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public double getEase() {
        return ease;
    }

    public void setEase(double ease) {
        this.ease = ease;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getRepetition() {
        return repetition;
    }

    public void setRepetition(int repetition) {
        this.repetition = repetition;
    }

    public String getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(String nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public String getLastReviewed() {
        return lastReviewed;
    }

    public void setLastReviewed(String lastReviewed) {
        this.lastReviewed = lastReviewed;
    }
}

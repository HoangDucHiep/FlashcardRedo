package com.cntt2.flashcard.data.remote.dto;

public class ReviewDto {
    private String id; // Nullable cho POST
    private String cardId;
    private double ease;
    private int interval;
    private int repetition;
    private String nextReviewDate;
    private String lastReviewed;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public double getEase() { return ease; }
    public void setEase(double ease) { this.ease = ease; }
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }
    public int getRepetition() { return repetition; }
    public void setRepetition(int repetition) { this.repetition = repetition; }
    public String getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(String nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public String getLastReviewed() { return lastReviewed; }
    public void setLastReviewed(String lastReviewed) { this.lastReviewed = lastReviewed; }
}
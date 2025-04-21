package com.cntt2.flashcard.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ReviewDto {
    @SerializedName("id")
    private String id;

    @SerializedName("cardId")
    private String cardId;

    @SerializedName("ease")
    private double ease;

    @SerializedName("interval")
    private int interval;

    @SerializedName("repetition")
    private int repetition;

    @SerializedName("nextReviewDate")
    private String nextReviewDate;

    @SerializedName("lastReviewed")
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
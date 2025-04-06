package com.cntt2.flashcard.model;

public class LearningSession {
    private int id;
    private int deskId;
    private String startTime;
    private String endTime;
    private int cardsStudied;
    private double performance; // percentage of correct answers

    public LearningSession() {}

    public LearningSession(int deskId, String startTime) {
        this.deskId = deskId;
        this.startTime = startTime;
        this.cardsStudied = 0;
        this.performance = 0.0;
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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getCardsStudied() {
        return cardsStudied;
    }

    public void setCardsStudied(int cardsStudied) {
        this.cardsStudied = cardsStudied;
    }

    public double getPerformance() {
        return performance;
    }

    public void setPerformance(double performance) {
        this.performance = performance;
    }
}

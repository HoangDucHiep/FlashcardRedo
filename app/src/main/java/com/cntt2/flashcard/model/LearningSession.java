package com.cntt2.flashcard.model;

public class LearningSession {
    private int id;
    private String serverId;
    private int deskId;
    private String startTime;
    private String endTime;
    private int cardsStudied;
    private double performance; // percentage of correct answers
    private String lastModified;
    private String syncStatus;

    public LearningSession() {}

    public LearningSession(int deskId, String startTime) {
        this.deskId = deskId;
        this.startTime = startTime;
        this.cardsStudied = 0;
        this.performance = 0.0;
    }

    public LearningSession(int deskId, String startTime, String endTime, int cardsStudied,
                           double performance, String lastModified, String syncStatus) {
        this.deskId = deskId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.cardsStudied = cardsStudied;
        this.performance = performance;
        this.lastModified = lastModified;
        this.syncStatus = syncStatus;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
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

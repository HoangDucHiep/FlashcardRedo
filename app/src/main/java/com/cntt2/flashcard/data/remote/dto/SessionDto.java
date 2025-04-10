package com.cntt2.flashcard.data.remote.dto;

public class SessionDto {
    private String id;
    private String deskId;
    private String startTime;
    private String endTime;
    private int cardsStudied;
    private double performance;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDeskId() { return deskId; }
    public void setDeskId(String deskId) { this.deskId = deskId; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public int getCardsStudied() { return cardsStudied; }
    public void setCardsStudied(int cardsStudied) { this.cardsStudied = cardsStudied; }
    public double getPerformance() { return performance; }
    public void setPerformance(double performance) { this.performance = performance; }
}
package com.cntt2.flashcard.data.remote.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class SessionDto {
    private String id;
    private String deskId;
    private String startTime; // Format: yyyy-MM-dd'T'HH:mm:ss.SSS
    private String endTime;   // Format: yyyy-MM-dd'T'HH:mm:ss.SSS, nullable
    private int cardsStudied;
    private double performance;

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    private String lastModified; // Format: yyyy-MM-dd'T'HH:mm:ss.SSS

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDeskId() { return deskId; }
    public void setDeskId(String deskId) {
        if (deskId == null || deskId.isEmpty()) {
            throw new IllegalArgumentException("DeskId cannot be null or empty");
        }
        this.deskId = deskId;
    }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) {
        if (startTime != null) {
            validateDateFormat(startTime);
        }
        this.startTime = startTime;
    }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) {
        if (endTime != null) {
            validateDateFormat(endTime);
        }
        this.endTime = endTime;
    }

    public int getCardsStudied() { return cardsStudied; }
    public void setCardsStudied(int cardsStudied) {
        if (cardsStudied < 0) {
            throw new IllegalArgumentException("Cards studied cannot be negative");
        }
        this.cardsStudied = cardsStudied;
    }

    public double getPerformance() { return performance; }
    public void setPerformance(double performance) {
        if (performance < 0 || performance > 100) {
            throw new IllegalArgumentException("Performance must be between 0 and 100");
        }
        this.performance = performance;
    }

    private void validateDateFormat(String dateStr) {
        try {
            dateFormat.parse(dateStr);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr +
                    ". Expected yyyy-MM-dd'T'HH:mm:ss.SSS");
        }
    }
}
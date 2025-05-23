package com.cntt2.flashcard.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CardDto {
    @SerializedName("id")
    private String id;

    @SerializedName("deskId")
    private String deskId;

    @SerializedName("front")
    private String front;

    @SerializedName("back")
    private String back;

    @SerializedName("imagePaths")
    private List<String> imagePaths = new ArrayList<>();

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("lastModified")
    private Date lastModified;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDeskId() { return deskId; }
    public void setDeskId(String deskId) { this.deskId = deskId; }
    public String getFront() { return front; }
    public void setFront(String front) { this.front = front; }
    public String getBack() { return back; }
    public void setBack(String back) { this.back = back; }
    public List<String> getImagePaths() { return imagePaths; }
    public void setImagePaths(List<String> imagePaths) { this.imagePaths = imagePaths; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
}
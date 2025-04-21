package com.cntt2.flashcard.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class ImageDto {
    @SerializedName("id")
    private String id;

    @SerializedName("fileName")
    private String fileName;

    @SerializedName("url")
    private String url;

    @SerializedName("uploadedAt")
    private Date uploadedAt;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Date getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Date uploadedAt) { this.uploadedAt = uploadedAt; }
}
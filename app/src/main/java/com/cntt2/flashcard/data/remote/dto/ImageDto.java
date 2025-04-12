package com.cntt2.flashcard.data.remote.dto;

import java.util.Date;

public class ImageDto {
    private String id;
    private String url;
    private Date uploadedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Date getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Date uploadedAt) { this.uploadedAt = uploadedAt; }
}
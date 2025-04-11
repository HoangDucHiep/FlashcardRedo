package com.cntt2.flashcard.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class DeskDto {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("isPublic")
    private boolean isPublic;

    @SerializedName("folderId")
    private String folderId;

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("lastModified")
    private Date lastModified;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
}
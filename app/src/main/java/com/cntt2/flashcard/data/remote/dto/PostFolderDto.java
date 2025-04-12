package com.cntt2.flashcard.data.remote.dto;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class PostFolderDto {
    @SerializedName("Name")
    private String name;

    @SerializedName("ParentFolderId")
    private String parentFolderId;

    @SerializedName("CreatedAt")
    private Date createdAt;

    @SerializedName("LastModified")
    private Date lastModified;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(String parentFolderId) { this.parentFolderId = parentFolderId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
}
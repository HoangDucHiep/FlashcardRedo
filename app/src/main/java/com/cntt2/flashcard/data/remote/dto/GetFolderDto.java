package com.cntt2.flashcard.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class GetFolderDto {
    @SerializedName("id") // Sửa từ "Id" thành "id"
    private String id;

    @SerializedName("name") // Sửa từ "Name" thành "name"
    private String name;

    @SerializedName("parentFolderId") // Sửa từ "ParentFolderId" thành "parentFolderId"
    private String parentFolderId;

    @SerializedName("createdAt") // Sửa từ "CreatedAt" thành "createdAt"
    private Date createdAt;

    @SerializedName("lastModified") // Sửa từ "LastModified" thành "lastModified"
    private Date lastModified;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}
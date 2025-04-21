package com.cntt2.flashcard.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class PostFolderDto {
    @SerializedName("name")
    private String name;

    @SerializedName("parentFolderId")
    private String parentFolderId;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(String parentFolderId) { this.parentFolderId = parentFolderId; }
}
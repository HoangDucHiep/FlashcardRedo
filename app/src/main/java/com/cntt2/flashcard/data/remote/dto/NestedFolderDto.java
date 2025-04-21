package com.cntt2.flashcard.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NestedFolderDto {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("parentFolderId")
    private String parentFolderId;

    @SerializedName("createdAt")
    private Date createdAt; // Đổi thành Date

    @SerializedName("lastModified")
    private Date lastModified; // Đổi thành Date

    @SerializedName("subFolders")
    private List<NestedFolderDto> subFolders = new ArrayList<>();

    @SerializedName("desks")
    private List<DeskDto> desks = new ArrayList<>();

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

    public List<NestedFolderDto> getSubFolders() {
        return subFolders;
    }

    public void setSubFolders(List<NestedFolderDto> subFolders) {
        this.subFolders = subFolders;
    }

    public List<DeskDto> getDesks() {
        return desks;
    }

    public void setDesks(List<DeskDto> desks) {
        this.desks = desks;
    }
}
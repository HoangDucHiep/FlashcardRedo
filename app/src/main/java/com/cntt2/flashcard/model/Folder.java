package com.cntt2.flashcard.model;

import java.util.ArrayList;
import java.util.List;

public class Folder {
    private int id;
    private Integer parentFolderId; // null for root folder
    private String name;
    private String createdAt;
    private boolean isExpanded = false;
    private List<Folder> subFolders = new ArrayList<>();
    private List<Desk> desks = new ArrayList<>();

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String displayName;

    public Folder() {}

    public Folder(String name, String createdAt) {
        this.name = name;
        this.createdAt = createdAt;
    }

    public Integer getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(Integer parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public List<Folder> getSubFolders() {
        return subFolders;
    }

    public void setSubFolders(List<Folder> subFolders) {
        this.subFolders = subFolders;
    }

    public void addSubFolder(Folder subFolder) {
        this.subFolders.add(subFolder);
    }

    public List<Desk> getDesks() {
        return desks;
    }

    public void setDesks(List<Desk> desks) {
        this.desks = desks;
    }

    public void addDesk(Desk desk) {
        this.desks.add(desk);
    }
}

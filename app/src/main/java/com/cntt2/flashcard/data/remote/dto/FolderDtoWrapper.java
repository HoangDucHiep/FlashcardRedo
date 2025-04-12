package com.cntt2.flashcard.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class FolderDtoWrapper {
    @SerializedName("folderDto")
    private GetFolderDto getFolderDto;

    public FolderDtoWrapper(GetFolderDto getFolderDto) {
        this.getFolderDto = getFolderDto;
    }

    public GetFolderDto getFolderDto() {
        return getFolderDto;
    }

    public void setFolderDto(GetFolderDto getFolderDto) {
        this.getFolderDto = getFolderDto;
    }
}
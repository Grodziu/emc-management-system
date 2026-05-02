package pl.emcmanagement.model;

import pl.emcmanagement.enums.StepMediaKind;

import java.util.Arrays;

public class StepMedia {
    private int id;
    private int legTestStepId;
    private StepMediaKind mediaKind;
    private String slotCode;
    private String displayName;
    private int sortOrder;
    private String fileName;
    private byte[] fileData;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLegTestStepId() {
        return legTestStepId;
    }

    public void setLegTestStepId(int legTestStepId) {
        this.legTestStepId = legTestStepId;
    }

    public StepMediaKind getMediaKind() {
        return mediaKind;
    }

    public void setMediaKind(StepMediaKind mediaKind) {
        this.mediaKind = mediaKind;
    }

    public String getSlotCode() {
        return slotCode;
    }

    public void setSlotCode(String slotCode) {
        this.slotCode = slotCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public byte[] getFileDataCopy() {
        return fileData == null ? null : Arrays.copyOf(fileData, fileData.length);
    }
}

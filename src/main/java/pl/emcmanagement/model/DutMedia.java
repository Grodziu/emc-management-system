package pl.emcmanagement.model;

import pl.emcmanagement.enums.DutMediaType;

import java.util.Arrays;

public class DutMedia {
    private int id;
    private int dutSampleId;
    private DutMediaType mediaType;
    private String fileName;
    private byte[] fileData;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDutSampleId() {
        return dutSampleId;
    }

    public void setDutSampleId(int dutSampleId) {
        this.dutSampleId = dutSampleId;
    }

    public DutMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(DutMediaType mediaType) {
        this.mediaType = mediaType;
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

package pl.emcmanagement.model;

import pl.emcmanagement.enums.AccreditationStatus;
import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.enums.TestType;

import java.time.LocalDate;

public class Leg {
    private int id;
    private int projectId;
    private String legCode;
    private TestType testType;
    private AccreditationStatus accreditation;
    private LocalDate startDate;
    private LocalDate endDate;
    private User assignedTt;
    private String isoStandardName;
    private String isoFilePath;
    private String isoFileName;
    private byte[] isoFileData;
    private String clientStandardName;
    private String clientFilePath;
    private String clientFileName;
    private byte[] clientFileData;
    private String testPlanName;
    private String testPlanFilePath;
    private String testPlanFileName;
    private byte[] testPlanFileData;
    private String pcaUrl;
    private TestStatus status;
    private int dutCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getLegCode() {
        return legCode;
    }

    public void setLegCode(String legCode) {
        this.legCode = legCode;
    }

    public TestType getTestType() {
        return testType;
    }

    public void setTestType(TestType testType) {
        this.testType = testType;
    }

    public AccreditationStatus getAccreditation() {
        return accreditation;
    }

    public void setAccreditation(AccreditationStatus accreditation) {
        this.accreditation = accreditation;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public User getAssignedTt() {
        return assignedTt;
    }

    public void setAssignedTt(User assignedTt) {
        this.assignedTt = assignedTt;
    }

    public String getIsoStandardName() {
        return isoStandardName;
    }

    public void setIsoStandardName(String isoStandardName) {
        this.isoStandardName = isoStandardName;
    }

    public String getIsoFilePath() {
        return isoFilePath;
    }

    public void setIsoFilePath(String isoFilePath) {
        this.isoFilePath = isoFilePath;
    }

    public String getIsoFileName() {
        return isoFileName;
    }

    public void setIsoFileName(String isoFileName) {
        this.isoFileName = isoFileName;
    }

    public byte[] getIsoFileData() {
        return isoFileData;
    }

    public void setIsoFileData(byte[] isoFileData) {
        this.isoFileData = isoFileData;
    }

    public String getClientStandardName() {
        return clientStandardName;
    }

    public void setClientStandardName(String clientStandardName) {
        this.clientStandardName = clientStandardName;
    }

    public String getClientFilePath() {
        return clientFilePath;
    }

    public void setClientFilePath(String clientFilePath) {
        this.clientFilePath = clientFilePath;
    }

    public String getClientFileName() {
        return clientFileName;
    }

    public void setClientFileName(String clientFileName) {
        this.clientFileName = clientFileName;
    }

    public byte[] getClientFileData() {
        return clientFileData;
    }

    public void setClientFileData(byte[] clientFileData) {
        this.clientFileData = clientFileData;
    }

    public String getTestPlanName() {
        return testPlanName;
    }

    public void setTestPlanName(String testPlanName) {
        this.testPlanName = testPlanName;
    }

    public String getTestPlanFilePath() {
        return testPlanFilePath;
    }

    public void setTestPlanFilePath(String testPlanFilePath) {
        this.testPlanFilePath = testPlanFilePath;
    }

    public String getTestPlanFileName() {
        return testPlanFileName;
    }

    public void setTestPlanFileName(String testPlanFileName) {
        this.testPlanFileName = testPlanFileName;
    }

    public byte[] getTestPlanFileData() {
        return testPlanFileData;
    }

    public void setTestPlanFileData(byte[] testPlanFileData) {
        this.testPlanFileData = testPlanFileData;
    }

    public String getPcaUrl() {
        return pcaUrl;
    }

    public void setPcaUrl(String pcaUrl) {
        this.pcaUrl = pcaUrl;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    public int getDutCount() {
        return dutCount;
    }

    public void setDutCount(int dutCount) {
        this.dutCount = dutCount;
    }
}

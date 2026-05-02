package pl.emcmanagement.dao;

import pl.emcmanagement.enums.ObservedFunctionalClass;
import pl.emcmanagement.enums.TestStatus;

import java.time.LocalDate;

public class DutResultRow {
    private int dutSampleId;
    private String sampleCode;
    private String serialNumber;
    private ObservedFunctionalClass observedFunctionalClass;
    private TestStatus resultStatus;
    private LocalDate executionDate;
    private String comment;

    public int getDutSampleId() {
        return dutSampleId;
    }

    public void setDutSampleId(int dutSampleId) {
        this.dutSampleId = dutSampleId;
    }

    public String getSampleCode() {
        return sampleCode;
    }

    public void setSampleCode(String sampleCode) {
        this.sampleCode = sampleCode;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public ObservedFunctionalClass getObservedFunctionalClass() {
        return observedFunctionalClass;
    }

    public void setObservedFunctionalClass(ObservedFunctionalClass observedFunctionalClass) {
        this.observedFunctionalClass = observedFunctionalClass;
    }

    public TestStatus getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(TestStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    public LocalDate getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDate executionDate) {
        this.executionDate = executionDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

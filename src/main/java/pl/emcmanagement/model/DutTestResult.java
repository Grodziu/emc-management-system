package pl.emcmanagement.model;

import pl.emcmanagement.enums.ObservedFunctionalClass;
import pl.emcmanagement.enums.TestStatus;

import java.time.LocalDate;

public class DutTestResult {
    private int id;
    private int dutSampleId;
    private int legTestStepId;
    private ObservedFunctionalClass observedFunctionalClass;
    private TestStatus resultStatus;
    private LocalDate executionDate;
    private String comment;

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

    public int getLegTestStepId() {
        return legTestStepId;
    }

    public void setLegTestStepId(int legTestStepId) {
        this.legTestStepId = legTestStepId;
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

package pl.emcmanagement.model;

import pl.emcmanagement.enums.TestStatus;

import java.time.LocalDate;

public class LegTestStep {
    private int id;
    private int legId;
    private int stepOrder;
    private String stepName;
    private TestStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String testRoom;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLegId() {
        return legId;
    }

    public void setLegId(int legId) {
        this.legId = legId;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
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

    public String getTestRoom() {
        return testRoom;
    }

    public void setTestRoom(String testRoom) {
        this.testRoom = testRoom;
    }
}

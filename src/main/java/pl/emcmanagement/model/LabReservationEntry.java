package pl.emcmanagement.model;

import java.time.LocalDate;

public class LabReservationEntry {
    private String roomCode;
    private LocalDate reservationDate;
    private String ewrNumber;
    private String legCode;
    private int stepOrder;
    private String stepName;
    private String status;

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDate reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getEwrNumber() {
        return ewrNumber;
    }

    public void setEwrNumber(String ewrNumber) {
        this.ewrNumber = ewrNumber;
    }

    public String getLegCode() {
        return legCode;
    }

    public void setLegCode(String legCode) {
        this.legCode = legCode;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayLabel() {
        return ewrNumber + " | " + legCode + " | " + stepOrder + ". " + stepName;
    }
}

package pl.emcmanagement.model;

import java.time.LocalDate;

public class EquipmentReservationEntry {
    private String equipmentCode;
    private String equipmentName;
    private String category;
    private String ewrNumber;
    private String legCode;
    private int stepOrder;
    private String stepName;
    private String roomCode;
    private LocalDate reservedFrom;
    private LocalDate reservedTo;
    private String status;

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public void setEquipmentCode(String equipmentCode) {
        this.equipmentCode = equipmentCode;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public LocalDate getReservedFrom() {
        return reservedFrom;
    }

    public void setReservedFrom(LocalDate reservedFrom) {
        this.reservedFrom = reservedFrom;
    }

    public LocalDate getReservedTo() {
        return reservedTo;
    }

    public void setReservedTo(LocalDate reservedTo) {
        this.reservedTo = reservedTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStepLabel() {
        return stepOrder + ". " + stepName;
    }
}

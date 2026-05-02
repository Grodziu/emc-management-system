package pl.emcmanagement.model;

import java.time.LocalDate;

public class MeasurementEquipment {
    private int id;
    private String equipmentCode;
    private String equipmentName;
    private String category;
    private String manufacturer;
    private String model;
    private String serialNumber;
    private LocalDate calibrationValidUntil;
    private boolean labOwned = true;
    private String location;
    private String notes;
    private String climateSensorCode;
    private String assignedStepsSummary;
    private LocalDate reservedFrom;
    private LocalDate reservedTo;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public LocalDate getCalibrationValidUntil() {
        return calibrationValidUntil;
    }

    public void setCalibrationValidUntil(LocalDate calibrationValidUntil) {
        this.calibrationValidUntil = calibrationValidUntil;
    }

    public boolean isLabOwned() {
        return labOwned;
    }

    public void setLabOwned(boolean labOwned) {
        this.labOwned = labOwned;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getClimateSensorCode() {
        return climateSensorCode;
    }

    public void setClimateSensorCode(String climateSensorCode) {
        this.climateSensorCode = climateSensorCode;
    }

    public String getAssignedStepsSummary() {
        return assignedStepsSummary;
    }

    public void setAssignedStepsSummary(String assignedStepsSummary) {
        this.assignedStepsSummary = assignedStepsSummary;
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

    @Override
    public String toString() {
        return equipmentCode + " | " + equipmentName;
    }
}

package pl.emcmanagement.model;

import pl.emcmanagement.enums.TestStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Project {
    private int id;
    private String ewrNumber;
    private String brand;
    private String deviceName;
    private String shortDescription;
    private User ve;
    private User te;
    private List<User> ttUsers = new ArrayList<>();
    private TestStatus status;
    private LocalDate startDate;
    private LocalDate endDate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEwrNumber() {
        return ewrNumber;
    }

    public void setEwrNumber(String ewrNumber) {
        this.ewrNumber = ewrNumber;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public User getVe() {
        return ve;
    }

    public void setVe(User ve) {
        this.ve = ve;
    }

    public User getTe() {
        return te;
    }

    public void setTe(User te) {
        this.te = te;
    }

    public List<User> getTtUsers() {
        return ttUsers;
    }

    public void setTtUsers(List<User> ttUsers) {
        this.ttUsers = ttUsers;
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

    public String getDisplayName() {
        return ewrNumber + " | " + brand + " " + deviceName + " " + shortDescription;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}

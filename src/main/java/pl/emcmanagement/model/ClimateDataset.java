package pl.emcmanagement.model;

import java.util.ArrayList;
import java.util.List;

public class ClimateDataset {
    private final List<ClimateMeasurement> measurements = new ArrayList<>();
    private final List<String> sourceFilenames = new ArrayList<>();
    private String roomCode;
    private String sensorCode;
    private String sourceDescription;

    public List<ClimateMeasurement> getMeasurements() {
        return measurements;
    }

    public List<String> getSourceFilenames() {
        return sourceFilenames;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getSensorCode() {
        return sensorCode;
    }

    public void setSensorCode(String sensorCode) {
        this.sensorCode = sensorCode;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public void setSourceDescription(String sourceDescription) {
        this.sourceDescription = sourceDescription;
    }
}

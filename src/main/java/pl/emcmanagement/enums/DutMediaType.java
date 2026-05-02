package pl.emcmanagement.enums;

public enum DutMediaType {
    FRONT_VIEW("Front view"),
    BACK_VIEW("Back view"),
    CONNECTOR_VIEW("Connector view"),
    LABEL("Label");

    private final String displayName;

    DutMediaType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

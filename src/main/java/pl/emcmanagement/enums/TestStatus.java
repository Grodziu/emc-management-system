package pl.emcmanagement.enums;

public enum TestStatus {
    NOT_STARTED("Not started"),
    ONGOING("Ongoing"),
    DATA_IN_ANALYSIS("Data in analysis"),
    PASSED("Passed"),
    FAILED("Failed");

    private final String displayName;

    TestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

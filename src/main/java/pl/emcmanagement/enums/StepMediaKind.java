package pl.emcmanagement.enums;

public enum StepMediaKind {
    SETUP("Setup"),
    VERIFICATION("Verification");

    private final String displayName;

    StepMediaKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

package pl.emcmanagement.enums;

public enum ObservedFunctionalClass {
    CLASS_A("Class A"),
    CLASS_B("Class B"),
    CLASS_C("Class C"),
    CLASS_D("Class D"),
    CLASS_E("Class E");

    private final String displayName;

    ObservedFunctionalClass(String displayName) {
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

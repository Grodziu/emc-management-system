package pl.emcmanagement.enums;

public enum StepSetupSlot {
    GENERAL_VIEW("SETUP_GENERAL_VIEW", "Test setup: General view", 1),
    CLOSER_VIEW("SETUP_CLOSER_VIEW", "Test setup: Closer view", 2),
    DUT_VIEW("SETUP_DUT_VIEW", "Test setup: DUT view", 3),
    AUXILIARY_EQUIPMENT("SETUP_AUXILIARY_EQUIPMENT", "Test setup: Auxiliary equipment", 4);

    private final String slotCode;
    private final String displayName;
    private final int sortOrder;

    StepSetupSlot(String slotCode, String displayName, int sortOrder) {
        this.slotCode = slotCode;
        this.displayName = displayName;
        this.sortOrder = sortOrder;
    }

    public String getSlotCode() {
        return slotCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}

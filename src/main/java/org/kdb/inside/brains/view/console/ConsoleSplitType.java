package org.kdb.inside.brains.view.console;

public enum ConsoleSplitType {
    NO("Do Not Split"),
    DOWN("Split Down"),
    RIGHT("Split Right");

    private final String label;

    ConsoleSplitType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
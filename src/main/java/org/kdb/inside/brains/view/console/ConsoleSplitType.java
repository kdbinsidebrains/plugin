package org.kdb.inside.brains.view.console;

public enum ConsoleSplitType {
    NO("Don't split"),
    VERTICAL("Split vertically"),
    HORIZONTAL("Split horizontally");

    private final String label;

    ConsoleSplitType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
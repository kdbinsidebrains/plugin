package org.kdb.inside.brains.view.console;

public enum ConsoleSplitType {
    NO("Don't split"),
    DOWN("Split vertically"),
    RIGHT("Split horizontally");

    private final String label;

    ConsoleSplitType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
package org.kdb.inside.brains.view.console;

public enum ConsoleSplitType {
    NO("Don't split"),
    VERTICAL("Split vertically"),
    HORIZONTAL("Split horizontally");

    private final String name;

    ConsoleSplitType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
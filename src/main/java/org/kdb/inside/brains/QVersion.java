package org.kdb.inside.brains;

public enum QVersion {
    VERSION_1("1.x", "Initial version"),
    VERSION_2("2.x", "Performance"),
    VERSION_3("3.x", "GUID, long is default"),
    VERSION_4("4.x", "Performance and security");

    private final String name;
    private final String description;

    public static final QVersion DEFAULT = VERSION_4;

    QVersion(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

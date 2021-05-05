package org.kdb.inside.brains;

public final class QEntity {
    final String name;
    final String description;
    final Type type;

    QEntity(String name, Type type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    enum Type {
        KEYWORD,
        FUNCTION,
        COMMAND
    }
}
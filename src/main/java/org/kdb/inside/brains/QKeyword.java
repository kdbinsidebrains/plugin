package org.kdb.inside.brains;

public final class QKeyword {
    final Type type;
    final String name;
    final String arguments;
    final String description;

    QKeyword(String name, Type type, String description) {
        this(name, type, null, description);
    }

    QKeyword(String name, Type type, String arguments, String description) {
        this.name = name;
        this.type = type;
        this.arguments = arguments;
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getArguments() {
        return arguments;
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
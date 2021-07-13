package org.kdb.inside.brains;

public final class QWord {
    private final Type type;
    private final String name;
    private final String arguments;
    private final String description;
    private final String docsUrl;

    QWord(String name, Type type, String arguments, String description, String docsUrl) {
        this.name = name;
        this.type = type;
        this.arguments = arguments;
        this.description = description;
        this.docsUrl = docsUrl;
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

    public String getDocsUrl() {
        return docsUrl;
    }

    public enum Type {
        KEYWORD,
        COMMAND,
        FUNCTION;

        private final String code;

        Type() {
            this.code = name().toLowerCase();
        }

        public String getCode() {
            return code;
        }
    }
}
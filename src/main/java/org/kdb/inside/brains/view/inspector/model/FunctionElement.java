package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;

public class FunctionElement extends ExecutableElement {
    private static final String[] TYPE_NAMES = {
            "function",
            "unary primitive",
            "operator",
            "iterator",
            "projection",
            "composition",
            "f'",
            "f/",
            "f\\",
            "f':",
            "f/:",
            "f\\:",
            "native function",
    };
    private final short type;
    private final String[] arguments;
    private final String location;

    public FunctionElement(String namespace, Object[] item) {
        super((String) item[0], namespace, KdbIcons.Node.Function);
        type = (short) item[1];
        arguments = (String[]) item[2];

        location = typeName("[" + String.join(", ", arguments) + "]", type);
    }

    private static String typeName(String arguments, short type) {
        if (100 == type) {
            return arguments;
        }
        if (101 == type) {
            return "unary" + arguments;
        }
        final int i = type - 100;
        if (i >= 0 && i < TYPE_NAMES.length) {
            return TYPE_NAMES[i];
        }
        return "";
    }

    public short getType() {
        return type;
    }

    public String[] getArguments() {
        return arguments;
    }

    @Override
    public @Nullable String getLocationString() {
        return location;
    }
}

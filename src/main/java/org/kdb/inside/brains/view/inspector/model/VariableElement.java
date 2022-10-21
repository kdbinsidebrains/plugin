package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;

public class VariableElement extends ExecutableElement {
    private final short type;
    private static final String[] TYPE_NAMES = {
            "list", // 0
            "boolean", // 1
            "guid", // 2
            null, // unused
            "byte", // 4
            "short", // 5
            "int", // 6
            "long", // 7
            "real",// 8
            "float", // 9
            "char", // 10
            "symbol", // 11
            "timestamp", // 12
            "month", // 13
            "date", // 14
            "datetime", // 15
            "timespan", // 16
            "minute", // 17
            "second", // 18
            "time" // 19
    };
    private final String location;

    public VariableElement(Object[] item) {
        super((String) item[0], KdbIcons.Node.Variable);
        type = (Short) item[1];
        location = typeName(type);
    }

    private static String typeName(int type) {
        if (type <= 0) {
            int l = -type;
            if (l < TYPE_NAMES.length - 1) {
                return TYPE_NAMES[l];
            }
            return "type " + type + "h";
        }

        if (type < TYPE_NAMES.length - 1) {
            return "list of " + TYPE_NAMES[type] + "s";
        }

        if (type >= 20 && type <= 76) {
            return "enum";
        }
        if (type == 77) {
            return "anymap";
        }
        if (type >= 78 && type <= 96) {
            return "mapped list of lists of " + typeName(type - 77);
        }
        if (type == 97) {
            return "sym enum";
        }
        if (type == 98) {
            return "table";
        }
        if (type == 99) {
            return "dictionary";
        }
        return "type " + type + "h";
    }

    public short getType() {
        return type;
    }

    @Override
    public @Nullable String getLocationString() {
        return location;
    }
}

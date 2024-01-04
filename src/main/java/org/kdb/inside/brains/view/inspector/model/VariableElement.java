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

    public VariableElement(String namespace, Object[] item) {
        super((String) item[0], namespace, KdbIcons.Node.Variable);
        type = ((Number) item[1]).shortValue();
        location = typeName(type, item[2]);
    }

    private static String typeName(int type, Object description) {
        if (type <= 0) {
            int l = -type;
            if (l < TYPE_NAMES.length - 1) {
                return TYPE_NAMES[l];
            }
            return "type " + type + "h";
        }

        if (type == 10) {
            return "string";
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
            return "mapped list of lists of " + typeName(type - 77, description);
        }
        if (type == 97) {
            return "sym enum";
        }
        if (type == 98) {
            return "table";
        }
        if (type == 99) {
            if (description == null) {
                return "dictionary";
            }
            final Object[] d = (Object[]) description;
            final long size = ((Number) d[0]).longValue();
            final int keysType = ((Number) d[1]).intValue();
            final int valuesType = ((Number) d[2]).intValue();
            return "dict " + size + "#(" + shortTypeName(keysType) + ")!(" + shortTypeName(valuesType) + ")";
        }
        return "type " + type + "h";
    }

    private static String shortTypeName(int type) {
        if (type <= 0) {
            int l = -type;
            if (l < TYPE_NAMES.length - 1) {
                return TYPE_NAMES[l];
            }
            return type + "h";
        }
        if (type < TYPE_NAMES.length - 1) {
            return TYPE_NAMES[type] + "s";
        }
        return typeName(type, null);
    }

    public short getType() {
        return type;
    }

    @Override
    public @Nullable String getLocationString() {
        return location;
    }
}

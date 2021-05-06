package org.kdb.inside.brains;

import java.util.HashMap;
import java.util.Map;

public enum KdbType {
    BOOLEAN("boolean", 'b', 1),
    BYTE("byte", 'x', 4),
    SHORT("short", 'h', 5),
    INT("int", 'i', 6),
    LONG("long", 'j', 7),
    REAL("real", 'e', 8),
    FLOAT("float", 'f', 9),
    CHAR("char", 'c', 10),
    SYMBOL("symbol", 's', 11),
    TIMESTAMP("timestamp", 'p', 12),
    MONTH("month", 'm', 13),
    DATE("date", 'd', 14),
    DATETIME("datetime", 'z', 15),
    TIMESPAN("timespan", 'n', 16),
    MINUTE("minute", 'u', 17),
    SECOND("second", 'v', 18),
    TIME("time", 't', 19),
    YEAR("year", ' ', 0);

    private final char code;
    private final String name;
    private final int type;

    private static final Map<String, KdbType> byName = new HashMap<>();
    private static final Map<Character, KdbType> byCode = new HashMap<>();

    static {
        final KdbType[] values = KdbType.values();
        for (KdbType value : values) {
            byName.put(value.name, value);
            byCode.put(value.code, value);
        }
    }

    KdbType(String name, char code, int type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    public char getCode() {
        return code;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static KdbType byName(String name) {
        return byName.get(name);
    }

    public static KdbType byCode(char code) {
        return byCode.get(code);
    }
}

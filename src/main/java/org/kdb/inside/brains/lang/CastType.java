package org.kdb.inside.brains.lang;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public enum CastType {
    BOOLEAN('b', "boolean"),
    GUID('g', "guid"),
    BYTE('x', "byte"),
    SHORT('h', "short"),
    INT('i', "int"),
    LONG('j', "long"),
    REAL('e', "real"),
    FLOAT('f', "float"),
    CHAR('c', "char"),
    SYMBOL('s', "symbol"),
    TIMESTAMP('p', "timestamp"),
    MONTH('m', "month"),
    DATE('d', "date"),
    DATETIME('z', "datetime"),
    TIMESPAN('n', "timespan"),
    MINUTE('u', "minute"),
    SECOND('v', "second"),
    TIME('t', "time"),
    YEAR(' ', "year");

    private static final Map<String, CastType> byName = new HashMap<>();
    private static final Map<Character, CastType> byCode = new HashMap<>();

    static {
        Stream.of(CastType.values()).forEach(value -> {
            byName.put(value.name, value);
            byCode.put(value.code, value);
        });
    }

    public final char code;
    public final String name;
    public final String lowerCode;
    public final String upperCode;

    CastType(char code, String name) {
        this.code = code;
        this.name = name;
        this.lowerCode = String.valueOf(Character.toLowerCase(code));
        this.upperCode = String.valueOf(Character.toUpperCase(code));
    }

    public static CastType byName(String name) {
        return byName.get(name);
    }

    public static CastType byCode(char code) {
        return byCode.get(code);
    }
}

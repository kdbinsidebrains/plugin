package org.kdb.inside.brains;

import com.google.gson.internal.Primitives;
import kx.c;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public enum KdbType implements Type {
    BOOLEAN(-1, 'b'),
    GUID(-2, 'g'),
    BYTE(-4, 'x'),
    SHORT(-5, 'h'),
    INT(-6, 'i'),
    LONG(-7, 'j'),
    REAL(-8, 'e'),
    FLOAT(-9, 'f'),
    CHAR(-10, 'c'),
    SYMBOL(-11, 's'),
    TIMESTAMP(-12, 'p'),
    MONTH(-13, 'm'),
    DATE(-14, 'd'),
    DATETIME(-15, 'z'),
    TIMESPAN(-16, 'n'),
    MINUTE(-17, 'u'),
    SECOND(-18, 'v'),
    TIME(-19, 't'),

    BOOLEAN_LIST(BOOLEAN),
    GUID_LIST(GUID),
    BYTE_LIST(BYTE),
    SHORT_LIST(SHORT),
    INT_LIST(INT),
    LONG_LIST(LONG),
    REAL_LIST(REAL),
    FLOAT_LIST(FLOAT),
    CHAR_LIST(CHAR),
    SYMBOL_LIST(SYMBOL),
    TIMESTAMP_LIST(TIMESTAMP),
    MONTH_LIST(MONTH),
    DATE_LIST(DATE),
    DATETIME_LIST(DATETIME),
    TIMESPAN_LIST(TIMESPAN),
    MINUTE_LIST(MINUTE),
    SECOND_LIST(SECOND),
    TIME_LIST(TIME),

    TABLE(98, null, null) {
        @Override
        public boolean isOfType(Object object) {
            return object instanceof c.Flip;
        }
    },
    KEYED_TABLE(null, null, null) {
        @Override
        public boolean isOfType(Object object) {
            if (object instanceof c.Dict d) {
                return d.x instanceof c.Flip && d.y instanceof c.Flip;
            }
            return false;
        }
    },
    DICTIONARY(99, null, null) {
        @Override
        public boolean isOfType(Object object) {
            return object instanceof c.Dict;
        }
    },

    ANY(null, ' ', null, Object.class) {
        @Override
        public boolean isOfType(Object object) {
            return true;
        }
    },
    ANY_LIST(0, ANY) {
        @Override
        public boolean isOfType(Object object) {
            if (object == null) {
                return false;
            }
            return object.getClass().isArray();
        }
    },

    OTHER(null, null, null);

    private final Integer id;
    private final Character code;
    private final String typeName;

    private final Object nullValue;
    private final Class<?> javaType;

    // For lists only
    private final KdbType atomType;
    private final Class<?> primitiveListType;

    private static final Map<String, KdbType> nameToType = new HashMap<>();
    private static final Map<Integer, KdbType> idToType = new HashMap<>();
    private static final Map<Class<?>, KdbType> classToType = new HashMap<>();
    private static final Map<Character, KdbType> codeToType = new HashMap<>();
    private static final Map<KdbType, KdbType> atomToList = new EnumMap<>(KdbType.class);

    static {
        for (KdbType type : KdbType.values()) {
            if (type.id != null) {
                idToType.put(type.id, type);
            }
            if (type.code != null) {
                codeToType.put(type.code, type);
            }
            if (type.typeName != null) {
                nameToType.put(type.typeName, type);
            }
            if (type.javaType != null) {
                classToType.put(type.javaType, type);
            }
            if (type.primitiveListType != null) {
                classToType.put(type.primitiveListType, type);
            }
            if (type.atomType != null) {
                atomToList.put(type.atomType, type);
            }
        }
    }

    KdbType(Integer id, Character code) {
        this(id, code, c.NULL(code));
    }

    KdbType(Integer id, Character code, Object nullValue) {
        this(id, code, nullValue, nullValue == null ? null : nullValue.getClass());
    }

    KdbType(Integer id, Character code, Object nullValue, Class<?> javaType) {
        this.id = id;
        this.code = code;
        this.typeName = typeName(this);

        this.nullValue = nullValue;
        this.javaType = javaType;

        this.atomType = null;
        this.primitiveListType = null;
    }

    KdbType(KdbType atomType) {
        this(-atomType.id, atomType);
    }

    KdbType(Integer id, KdbType atomType) {
        this.id = id;
        this.atomType = atomType;
        this.typeName = typeName(this);

        this.code = atomType.code != null && atomType.code != ' ' ? Character.toUpperCase(atomType.code) : null;
        final Class<?> atomClass = atomType.getNull() != null ? atomType.getNull().getClass() : Object.class;

        final Object atomsArray = Array.newInstance(atomClass, 0);

        Class<?> primitiveClass = null;
        try {
            primitiveClass = (Class<?>) atomClass.getField("TYPE").get(null);
        } catch (Exception ignore) {
            // nothing to do. It's not primitive class.
        }

        if (primitiveClass != null) {
            Object primitiveArray = Array.newInstance(primitiveClass, 0);
            this.javaType = primitiveArray.getClass();
            this.primitiveListType = atomsArray.getClass();
            this.nullValue = primitiveArray;
        } else {
            this.javaType = atomsArray.getClass();
            this.primitiveListType = null;
            this.nullValue = atomsArray;
        }
    }

    private static String typeName(KdbType type) {
        final String name = type.name().toLowerCase();
        int i = name.indexOf('_');
        if (i > 0) {
            return name.substring(0, i) + "[]";
        }
        return name;
    }

    public Integer getId() {
        return this.id;
    }

    public Character getCode() {
        return this.code;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public KdbType getAtomType() {
        return atomType;
    }

    @SuppressWarnings("unchecked")
    public <T> T getNull() {
        return (T) nullValue;
    }

    public boolean isList() {
        return atomType != null;
    }

    public boolean isPrimitiveList() {
        return primitiveListType != null;
    }

    public boolean isOfType(Object object) {
        Object aNull = getNull();
        return object != null && aNull != null && aNull.getClass().isAssignableFrom(object.getClass());
    }

    public <T> T nullSafe(T data) {
        return data == null ? getNull() : data;
    }

    public static KdbType typeOf(int id) {
        return idToType.get(id);
    }

    public static KdbType typeOf(char code) {
        return codeToType.get(code);
    }

    public static KdbType typeOf(String name) {
        return nameToType.get(name);
    }

    public static KdbType typeOf(Object obj) {
        if (obj == null) {
            return ANY;
        }
        return typeOf(obj.getClass());
    }

    public static KdbType typeOf(Class<?> clazz) {
        final KdbType kdbType;
        if (clazz.isPrimitive()) {
            kdbType = classToType.get(Primitives.wrap(clazz));
        } else {
            kdbType = classToType.get(clazz);
        }
        return kdbType == null ? ANY : kdbType;
    }

    public static KdbType listOf(KdbType type) {
        return atomToList.getOrDefault(type, ANY_LIST);
    }

    public static boolean isNull(Object v) {
        return c.qn(v);
    }
}
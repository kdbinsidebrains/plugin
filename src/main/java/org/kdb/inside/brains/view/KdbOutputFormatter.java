package org.kdb.inside.brains.view;

import kx.KxConnection;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.console.ConsoleOptions;

import java.lang.reflect.Array;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.function.Function;

public final class KdbOutputFormatter {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd'T'HH:mm:ss.SSS");
    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy.MM.dd'D'HH:mm:ss");

    private static final DecimalFormat[] DECIMAL_FORMAT = new DecimalFormat[ConsoleOptions.MAX_DECIMAL_PRECISION + 1];

    static {
        DATE_FORMAT.setTimeZone(KxConnection.UTC_TIMEZONE);
        TIME_FORMAT.setTimeZone(KxConnection.UTC_TIMEZONE);
        DATETIME_FORMAT.setTimeZone(KxConnection.UTC_TIMEZONE);
        TIMESTAMP_FORMAT.setTimeZone(KxConnection.UTC_TIMEZONE);

        DECIMAL_FORMAT[0] = new DecimalFormat("0.");
        for (int i = 1; i <= ConsoleOptions.MAX_DECIMAL_PRECISION; i++) {
            DECIMAL_FORMAT[i] = new DecimalFormat("0." + "#".repeat(i));
        }
    }

    private final ConsoleOptions options;

    private static KdbOutputFormatter instance;

    public KdbOutputFormatter(ConsoleOptions options) {
        this.options = options;
    }

    public String resultToString(KdbResult result) {
        return formatObject(result.getObject());
    }

    public String objectToString(Object object) {
        return formatObject(object);
    }

    public static KdbOutputFormatter getInstance() {
        if (instance == null) {
            instance = new KdbOutputFormatter(KdbSettingsService.getInstance().getConsoleOptions());
        }
        return instance;
    }

    private String formatObject(Object v) {
        if (v == null) {
            return formatNull();
        }

        if (v instanceof String) {
            return formatSymbol((String) v);
        }
        if (v instanceof String[]) {
            return formatSymbol((String[]) v);
        }

        if (v instanceof Character) {
            return formatChar((Character) v);
        }
        if (v instanceof char[]) {
            return formatChar((char[]) v);
        }

        if (v instanceof Boolean) {
            return formatBool((Boolean) v);
        }
        if (v instanceof boolean[]) {
            return formatBool((boolean[]) v);
        }

        if (v instanceof Byte) {
            return formatByte((Byte) v);
        }
        if (v instanceof byte[]) {
            return formatByte((byte[]) v);
        }

        if (v instanceof Short) {
            return formatShort((Short) v);
        }
        if (v instanceof short[]) {
            return formatShort((short[]) v);
        }

        if (v instanceof Integer) {
            return formatInt((Integer) v);
        }
        if (v instanceof int[]) {
            return formatInt((int[]) v);
        }

        if (v instanceof Long) {
            return formatLong((Long) v);
        }
        if (v instanceof long[]) {
            return formatLong((long[]) v);
        }

        if (v instanceof Float) {
            return formatFloat((Float) v);
        }
        if (v instanceof float[]) {
            return formatFloat((float[]) v);
        }

        if (v instanceof Double) {
            return formatDouble((Double) v);
        }
        if (v instanceof double[]) {
            return formatDouble((double[]) v);
        }

        if (v instanceof Timestamp) {
            return formatTimestamp((Timestamp) v);
        }
        if (v instanceof Timestamp[]) {
            return formatArray(v, this::formatTimestamp, 16);
        }

        if (v instanceof UUID) {
            return formatUUID((UUID) v);
        }
        if (v instanceof UUID[]) {
            return formatArray(v, this::formatUUID, 36);
        }

        if (v instanceof Date) {
            return formatDate((Date) v);
        }
        if (v instanceof Date[]) {
            return formatArray(v, this::formatDate, 10);
        }

        if (v instanceof Time) {
            return formatTime((Time) v);
        }
        if (v instanceof Time[]) {
            return formatArray(v, this::formatTime, 12);
        }

        if (v instanceof java.util.Date) {
            return formatDatetime((java.util.Date) v);
        }
        if (v instanceof java.util.Date[]) {
            return formatArray(v, this::formatDatetime, 23);
        }

        if (v instanceof c.Timespan) {
            return formatTimespan((c.Timespan) v);
        }
        if (v instanceof c.Timespan[]) {
            return formatArray(v, this::formatTimespan, 12);
        }

        if (v instanceof c.Month) {
            return formatMonth((c.Month) v);
        }
        if (v instanceof c.Month[]) {
            return formatArray(v, this::formatMonth, 12);
        }

        if (v instanceof c.Minute) {
            return formatMinute((c.Minute) v);
        }
        if (v instanceof c.Minute[]) {
            return formatArray(v, this::formatMinute, 12);
        }

        if (v instanceof c.Second) {
            return formatSecond((c.Second) v);
        }
        if (v instanceof c.Second[]) {
            return formatArray(v, this::formatSecond, 12);
        }

        if (v instanceof c.Flip) {
            return formatFlip((c.Flip) v);
        }

        if (v instanceof c.Dict) {
            return formatDict((c.Dict) v);
        }

        if (v instanceof c.Operator) {
            return ((c.Operator) v).getOperation();
        }

        if (v instanceof c.Function) {
            return ((c.Function) v).getContent();
        }

        if (v instanceof c.Projection) {
            return formatProjection((c.Projection) v);
        }

        if (v instanceof c.Composition) {
            return formatComposition((c.Composition) v);
        }

        if (v instanceof c.Each) {
            return formatEach((c.Each) v);
        }

        if (v.getClass().isArray()) {
            return '(' + formatArray(v, this::objectToString, 10, ';') + ')';
        }

        return String.valueOf(v);
    }

    private String formatEach(c.Each v) {
        return objectToString(v.getArgument()) + v.getCode();
    }

    private String formatComposition(c.Composition v) {
        StringBuilder b = new StringBuilder();
        for (Object item : v.getItems()) {
            b.append(objectToString(item));
        }
        return b.toString();
    }

    private String formatProjection(c.Projection projection) {
        final Object[] args = projection.getItems();
        if (args.length == 0) {
            return "[]";
        }

        final Object first = args[0];
        boolean listProjection = false;
        if (first instanceof c.UnaryOperator) {
            final c.UnaryOperator up = (c.UnaryOperator) first;
            if (up.getType() == 41) // plist
                listProjection = true;
        }

        final StringBuilder b = new StringBuilder();
        if (listProjection) {
            if (args.length == 1) {
                return "()";
            }
            b.append("(");
            for (int i = 1; i < args.length; i++) {
                b.append(objectToString(args[i]));
                b.append(';');
            }
            b.setCharAt(b.length() - 1, ')');
            return b.toString();
        }

        final boolean function = (first instanceof c.Function) || (first instanceof c.UnaryOperator) || (first instanceof c.BinaryOperator);
        if (function) {
            b.append(objectToString(first));
            b.append('[');
        } else {
            b.append('(');
        }
        for (int i = function ? 1 : 0; i < args.length; i++) {
            b.append(objectToString(args[i]));
            b.append(";");
        }
        b.setCharAt(b.length() - 1, function ? ']' : ')');
        return b.toString();
    }

    @NotNull
    public String formatNull() {
        return "(::)";
    }


    @NotNull
    public String formatSymbol(String v) {
        return '`' + v;
    }

    @NotNull
    public String formatSymbol(String[] v) {
        if (v.length == 0) {
            return emptyArray(String.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + "`" + v[0];
        }
        return "`" + String.join("`", v);
    }

    @NotNull
    public String formatChar(char v) {
        return '"' + String.valueOf(v) + '"';
    }

    @NotNull
    public String formatChar(char[] v) {
        final String s = '"' + String.valueOf(v) + '"';
        return v.length == 1 ? oneItemArrayPrefix() + s : s;
    }

    @NotNull
    public String formatBool(boolean v) {
        return (v ? '1' : '0') + "b";
    }

    @NotNull
    public String formatBool(boolean[] v) {
        if (v.length == 0) {
            return emptyArray(boolean.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + formatBool(v[0]);
        }

        final StringBuilder b = new StringBuilder(v.length + 1);
        for (boolean bl : v) {
            b.append(bl ? '1' : '0');
        }
        b.append('b');
        return b.toString();
    }

    @NotNull
    public String formatByte(byte v) {
        final StringBuilder b = new StringBuilder(4);
        b.append("0x");
        byteToStr(b, v);
        return b.toString();
    }

    @NotNull
    public String formatByte(byte[] v) {
        if (v.length == 0) {
            return emptyArray(byte.class);
        }

        if (v.length == 1) {
            return oneItemArrayPrefix() + formatByte(v[0]);
        }

        final StringBuilder b = new StringBuilder(v.length * 2 + 2);
        b.append("0x");
        for (byte b1 : v) {
            byteToStr(b, b1);
        }
        return b.toString();
    }

    @NotNull
    public String formatShort(short v) {
        return isNull(v) ? "0Nh" : v + "h";
    }

    @NotNull
    public String formatShort(short[] k) {
        if (k.length == 0) {
            return emptyArray(short.class);
        }
        if (k.length == 1) {
            return oneItemArrayPrefix() + formatShort(k[0]);
        }
        final StringBuilder b = new StringBuilder();
        for (short s : k) {
            b.append(isNull(s) ? "0N" : s);
            b.append(" ");
        }
        b.setCharAt(b.length() - 1, 'h');
        return b.toString();
    }

    @NotNull
    public String formatInt(int v) {
        return isNull(v) ? "0Ni" : v + "i";
    }

    @NotNull
    public String formatInt(int[] v) {
        if (v.length == 0) {
            return emptyArray(int.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + formatInt(v[0]);
        }
        final StringBuilder b = new StringBuilder();
        for (int i : v) {
            b.append(isNull(i) ? "0N" : i);
            b.append(" ");
        }
        b.setCharAt(b.length() - 1, 'i');
        return b.toString();
    }

    @NotNull
    public String formatLong(long v) {
        return (isNull(v) ? "0N" : String.valueOf(v));
    }

    @NotNull
    public String formatLong(long[] v) {
        if (v.length == 0) {
            return emptyArray(long.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + formatLong(v[0]);
        }
        final StringBuilder b = new StringBuilder();
        for (long l : v) {
            b.append(isNull(l) ? "0N" : l);
            b.append(" ");
        }
        b.setLength(b.length() - 1);
        return b.toString();
    }

    @NotNull
    public String formatFloat(float v) {
        return isNull(v) ? "0Ne" : DECIMAL_FORMAT[options.getFloatPrecision()].format(v) + "e";
    }

    @NotNull
    public String formatFloat(float[] v) {
        if (v.length == 0) {
            return emptyArray(float.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + formatFloat(v[0]);
        }
        final StringBuilder b = new StringBuilder();
        final DecimalFormat decimalFormat = DECIMAL_FORMAT[options.getFloatPrecision()];
        for (float f : v) {
            b.append(Float.isNaN(f) ? "0N" : decimalFormat.format(f));
            b.append(" ");
        }
        b.setCharAt(b.length() - 1, 'e');
        return b.toString();
    }

    @NotNull
    public String formatDouble(double v) {
        if (v % 1 == 0) {
            return ((long) v) + "f";
        }
        return isNull(v) ? "0n" : DECIMAL_FORMAT[options.getFloatPrecision()].format(v);
    }

    @NotNull
    public String formatDouble(double[] v) {
        if (v.length == 0) {
            return emptyArray(double.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + formatDouble(v[0]);
        }

        boolean postfix = true;
        final StringBuilder b = new StringBuilder();
        final DecimalFormat decimalFormat = DECIMAL_FORMAT[options.getFloatPrecision()];
        for (double d : v) {
            postfix &= d % 1 == 0;
            b.append(Double.isNaN(d) ? "0n" : decimalFormat.format(d));
            b.append(" ");
        }
        if (postfix) {
            b.setCharAt(b.length() - 1, 'f');
        } else {
            b.setLength(b.length() - 1);
        }
        return b.toString();
    }

    @NotNull
    public String formatTimestamp(Timestamp v) {
        if (isNull(v)) {
            return "0Np";
        }

        StringBuilder b = new StringBuilder(10);
        b.append('.');
        b.append(v.getNanos());
        while (b.length() < 10) {
            b.insert(1, "0");
        }
        return TIMESTAMP_FORMAT.format(v) + b;
    }

    @NotNull
    public String formatUUID(UUID v) {
        return isNull(v) ? "0Ng" : String.valueOf(v);
    }

    @NotNull
    public String formatDate(Date v) {
        return isNull(v) ? "0Nd" : DATE_FORMAT.format(v);
    }

    @NotNull
    public String formatTime(Time v) {
        return isNull(v) ? "0Nt" : TIME_FORMAT.format(v);
    }

    @NotNull
    public String formatDatetime(java.util.Date v) {
        return isNull(v) ? "0Nz" : DATETIME_FORMAT.format(v);
    }

    @NotNull
    public String formatTimespan(c.Timespan v) {
        return isNull(v) ? "0Nn" : String.valueOf(v);
    }

    @NotNull
    public String formatMonth(c.Month v) {
        return isNull(v) ? "0Nm" : String.valueOf(v);
    }

    @NotNull
    public String formatMinute(c.Minute v) {
        return isNull(v) ? "0Nu" : String.valueOf(v);
    }

    @NotNull
    public String formatSecond(c.Second v) {
        return isNull(v) ? "0Nv" : String.valueOf(v);
    }

    @NotNull
    public String formatFlip(c.Flip f) {
        final String[] x = f.x;
        final Object[] y = f.y;

        final StringBuilder b = new StringBuilder();

        b.append(Array.getLength(y[0]));
        b.append("#([]");
        for (int i = 0; i < x.length; i++) {
            final String name = x[i];
            final Object val = y[i];

            b.append(name);
            b.append(":");

            final String str = getKdbTypeName(val.getClass().getComponentType());
            if (str != null) {
                b.append("`").append(str).append("$");
            }
            b.append("(); ");
        }
        b.setLength(b.length() - 2);
        b.append(")");
        return b.toString();
    }

    @NotNull
    public String formatDict(c.Dict d) {
        String k = objectToString(d.x);
        String v = objectToString(d.y);
        return wrapArrayIfRequired(k) + '!' + wrapArrayIfRequired(v);
    }


    @NotNull
    private String emptyArray(Class<?> type) {
        return "`" + getKdbTypeName(type) + "$()";
    }

    private String oneItemArrayPrefix() {
        return options.isEnlistArrays() ? "enlist " : ",";
    }

    private <T> String formatArray(Object arr, Function<T, String> f, int itemLength) {
        return formatArray(arr, f, itemLength, ' ');
    }

    @SuppressWarnings("unchecked")
    private <T> String formatArray(Object arr, Function<T, String> f, int itemLength, char separator) {
        final int length = Array.getLength(arr);
        if (length == 0) {
            if (arr.getClass().getComponentType().equals(Object.class)) {
                return "";
            }
            return emptyArray(arr.getClass().getComponentType());
        }
        if (length == 1) {
            return oneItemArrayPrefix() + f.apply((T) Array.get(arr, 0));
        }
        StringBuilder b = new StringBuilder(itemLength * length + length);
        for (int i = 0; i < length; i++) {
            final Object obj = Array.get(arr, i);
            b.append(f.apply((T) obj));
            b.append(separator);
        }
        b.setLength(b.length() - 1);
        return b.toString();
    }

    private String wrapArrayIfRequired(String v) {
        final char c = v.charAt(0);
        if (c != '`' && v.charAt(0) != '(') {
            return '(' + v + ')';
        }
        return v;
    }

    private void byteToStr(StringBuilder b, byte b1) {
        int v = b1 & 0xFF;
        b.append(HEX_ARRAY[v >>> 4]);
        b.append(HEX_ARRAY[v & 0x0F]);
    }

    private boolean isNull(short v) {
        return v == Short.MIN_VALUE;
    }

    private boolean isNull(int v) {
        return v == Integer.MIN_VALUE;
    }

    private boolean isNull(long v) {
        return v == Long.MIN_VALUE;
    }

    private boolean isNull(float v) {
        return Float.isNaN(v);
    }

    private boolean isNull(double v) {
        return Double.isNaN(v);
    }

    private boolean isNull(Object v) {
        return c.qn(v);
    }

    private String getKdbTypeName(Class<?> aClass) {
        final KdbType kdbType = KdbType.typeOf(aClass);
        return kdbType == null || kdbType == KdbType.ANY ? null : kdbType.getTypeName();
    }
}

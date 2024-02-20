package org.kdb.inside.brains.view;

import kx.KxConnection;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.view.console.NumericalOptions;

import java.lang.reflect.Array;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.*;
import java.util.UUID;
import java.util.function.Function;

public final class KdbOutputFormatter {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd'T'HH:mm:ss.SSS");
    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy.MM.dd'D'HH:mm:ss");

    private static final DecimalFormat INTEGER = new DecimalFormat("0");
    private static final DecimalFormat INTEGER_SEPARATOR = new DecimalFormat("#,##0");

    private static final DecimalFormat[][] DECIMAL = new DecimalFormat[RoundingMode.values().length][NumericalOptions.MAX_DECIMAL_PRECISION + 1];
    private static final DecimalFormat[][] DECIMAL_SEPARATOR = new DecimalFormat[RoundingMode.values().length][NumericalOptions.MAX_DECIMAL_PRECISION + 1];
    private static final DecimalFormat[][] DECIMAL_SCIENTIFIC = new DecimalFormat[RoundingMode.values().length][NumericalOptions.MAX_DECIMAL_PRECISION + 1];

    private static KdbOutputFormatter defaultInstance;

    static {
        DATE_FORMAT.setTimeZone(KxConnection.UTC_TIMEZONE);
        TIME_FORMAT.setTimeZone(KxConnection.UTC_TIMEZONE);
        DATETIME_FORMAT.setTimeZone(KxConnection.UTC_TIMEZONE);
        TIMESTAMP_FORMAT.setTimeZone(KxConnection.UTC_TIMEZONE);

        for (RoundingMode mode : RoundingMode.values()) {
            final int modeId = mode.ordinal();
            for (int precision = 0; precision < NumericalOptions.MAX_DECIMAL_PRECISION; precision++) {
                DECIMAL[modeId][precision] = new DecimalFormat("0." + "#".repeat(precision));
                DECIMAL[modeId][precision].setRoundingMode(mode);

                DECIMAL_SEPARATOR[modeId][precision] = new DecimalFormat("#,##0." + "#".repeat(precision));
                DECIMAL_SEPARATOR[modeId][precision].setRoundingMode(mode);

                DECIMAL_SCIENTIFIC[modeId][precision] = createExponentialFormat("0." + "#".repeat(precision));
                DECIMAL_SCIENTIFIC[modeId][precision].setRoundingMode(mode);
            }
        }
    }

    private final FormatterOptions options;

    public KdbOutputFormatter(FormatterOptions options) {
        this.options = options;
    }

    public String objectToString(Object object) {
        return objectToString(object, options.isPrefixSymbols(), options.isWrapStrings());
    }

    public String objectToString(Object object, boolean prefixSymbol, boolean wrapString) {
        if (!prefixSymbol && object instanceof String) {
            return String.valueOf(object);
        } else if (!wrapString && object instanceof char[]) {
            return new String((char[]) object);
        } else if (!wrapString && object instanceof Character) {
            return String.valueOf(object);
        }
        return formatObject(object);
    }

    public String resultToString(KdbResult result, boolean prefixSymbol, boolean wrapString) {
        return objectToString(result.getObject(), prefixSymbol, wrapString);
    }

    /**
     * Returns formatter with default console options.
     */
    public static KdbOutputFormatter getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new KdbOutputFormatter(new FormatterOptions());
        }
        return defaultInstance;
    }

    private String formatObject(Object v) {
        if (v == null) {
            return formatNull();
        }

        if (v instanceof String) {
            return formatSymbol((String) v);
        }
        if (v instanceof String[]) {
            return formatSymbols((String[]) v);
        }

        if (v instanceof Character) {
            return formatChar((Character) v);
        }
        if (v instanceof char[]) {
            return formatChars((char[]) v);
        }

        if (v instanceof Boolean) {
            return formatBool((Boolean) v);
        }
        if (v instanceof boolean[]) {
            return formatBools((boolean[]) v);
        }

        if (v instanceof Byte) {
            return formatByte((Byte) v);
        }
        if (v instanceof byte[]) {
            return formatBytes((byte[]) v);
        }

        if (v instanceof Short) {
            return formatShort((Short) v);
        }
        if (v instanceof short[]) {
            return formatShorts((short[]) v);
        }

        if (v instanceof Integer) {
            return formatInt((Integer) v);
        }
        if (v instanceof int[]) {
            return formatInts((int[]) v);
        }

        if (v instanceof Long) {
            return formatLong((Long) v);
        }
        if (v instanceof long[]) {
            return formatLongs((long[]) v);
        }

        if (v instanceof Float) {
            return formatFloat((Float) v);
        }
        if (v instanceof float[]) {
            return formatFloats((float[]) v);
        }

        if (v instanceof Double) {
            return formatDouble((Double) v);
        }
        if (v instanceof double[]) {
            return formatDoubles((double[]) v);
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

        if (v instanceof c.EachIterator) {
            return formatEach((c.EachIterator) v);
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

        if (v.getClass().isArray()) {
            return '(' + formatArray(v, this::formatObject, 10, ';') + ')';
        }

        return String.valueOf(v);
    }

    private String formatEach(c.EachIterator v) {
        return formatObject(v.getArgument()) + v.getOperation();
    }

    private String formatComposition(c.Composition v) {
        StringBuilder b = new StringBuilder();
        for (Object item : v.getItems()) {
            b.append(formatObject(item));
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
        if (first instanceof c.UnaryOperator up) {
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
                b.append(formatObject(args[i]));
                b.append(';');
            }
            b.setCharAt(b.length() - 1, ')');
            return b.toString();
        }

        final boolean function = (first instanceof c.Function) || (first instanceof c.UnaryOperator) || (first instanceof c.BinaryOperator);
        if (function) {
            b.append(formatObject(first));
            b.append('[');
        } else {
            b.append('(');
        }
        for (int i = function ? 1 : 0; i < args.length; i++) {
            b.append(formatObject(args[i]));
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
    public String formatSymbols(String[] v) {
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
    public String formatChars(char[] v) {
        final String s = '"' + String.valueOf(v) + '"';
        return v.length == 1 ? oneItemArrayPrefix() + s : s;
    }

    @NotNull
    public String formatBool(boolean v) {
        return (v ? '1' : '0') + "b";
    }

    @NotNull
    public String formatBools(boolean[] v) {
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
    public String formatBytes(byte[] v) {
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
        return isNull(v) ? "0Nh" : shortToStr(v) + "h";
    }

    @NotNull
    public String formatShorts(short[] k) {
        if (k.length == 0) {
            return emptyArray(short.class);
        }
        if (k.length == 1) {
            return oneItemArrayPrefix() + formatShort(k[0]);
        }
        final StringBuilder b = new StringBuilder();
        for (short s : k) {
            b.append(isNull(s) ? "0N" : shortToStr(s));
            b.append(" ");
        }
        b.setCharAt(b.length() - 1, 'h');
        return b.toString();
    }

    @NotNull
    public String formatInt(int v) {
        return isNull(v) ? "0Ni" : intToStr(v) + "i";
    }

    @NotNull
    public String formatInts(int[] v) {
        if (v.length == 0) {
            return emptyArray(int.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + formatInt(v[0]);
        }
        final StringBuilder b = new StringBuilder();
        for (int i : v) {
            b.append(isNull(i) ? "0N" : intToStr(i));
            b.append(" ");
        }
        b.setCharAt(b.length() - 1, 'i');
        return b.toString();
    }

    @NotNull
    public String formatLong(long v) {
        return (isNull(v) ? "0N" : longToStr(v));
    }

    @NotNull
    public String formatLongs(long[] v) {
        if (v.length == 0) {
            return emptyArray(long.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + formatLong(v[0]);
        }
        final StringBuilder b = new StringBuilder();
        for (long l : v) {
            b.append(isNull(l) ? "0N" : longToStr(l));
            b.append(" ");
        }
        b.setLength(b.length() - 1);
        return b.toString();
    }

    @NotNull
    public String formatFloat(float v) {
        return isNull(v) ? "0Ne" : floatToStr(v) + "e";
    }

    @NotNull
    public String formatFloats(float[] v) {
        if (v.length == 0) {
            return emptyArray(float.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + formatFloat(v[0]);
        }
        final StringBuilder b = new StringBuilder();
        for (float f : v) {
            b.append(Float.isNaN(f) ? "0N" : floatToStr(f));
            b.append(" ");
        }
        b.setCharAt(b.length() - 1, 'e');
        return b.toString();
    }

    private static DecimalFormat createExponentialFormat(String pattern) {
        final DecimalFormat decimalFormat = new DecimalFormat(pattern + "E000") {
            @Override
            public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
                final StringBuffer format = super.format(number, result, fieldPosition);
                if (number < -1 || number > 1) {
                    format.insert(format.length() - 3, "+");
                }
                return format;
            }
        };

        final DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
        symbols.setExponentSeparator("e");
        decimalFormat.setDecimalFormatSymbols(symbols);

        return decimalFormat;
    }

    @NotNull
    public String formatDoubles(double[] v) {
        if (v.length == 0) {
            return emptyArray(double.class);
        }
        if (v.length == 1) {
            return oneItemArrayPrefix() + formatDouble(v[0]);
        }

        boolean postfix = true;
        final StringBuilder b = new StringBuilder();
        for (double d : v) {
            postfix &= d % 1 == 0;
            b.append(isNull(d) ? "0n" : doubleToStr(d));
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
        String k = formatObject(d.x);
        String v = formatObject(d.y);
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

    private String shortToStr(short s) {
        return options.isThousandsSeparator() ? longToStr(s) : String.valueOf(s);
    }

    private String intToStr(int s) {
        return options.isThousandsSeparator() ? longToStr(s) : String.valueOf(s);
    }

    @NotNull
    public String formatDouble(double v) {
        if (v % 1 == 0) {
            if (isScientificNotation(v)) {
                return doubleToStr(v);
            }
            return ((long) v) + "f";
        }
        return isNull(v) ? "0n" : doubleToStr(v);
    }

    private String floatToStr(float s) {
        return doubleToStr(s);
    }

    private String longToStr(long s) {
        return (options.isThousandsSeparator() ? INTEGER_SEPARATOR : INTEGER).format(s);
    }

    private String doubleToStr(double s) {
        final DecimalFormat[][] formats;
        if (isScientificNotation(s)) {
            formats = DECIMAL_SCIENTIFIC;
        } else {
            formats = options.isThousandsSeparator() ? DECIMAL_SEPARATOR : DECIMAL;
        }
        return formats[options.getRoundingMode().ordinal()][options.getFloatPrecision()].format(s);
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
        return KdbType.isNull(v);
    }

    private String getKdbTypeName(Class<?> aClass) {
        final KdbType kdbType = KdbType.typeOf(aClass);
        return kdbType == null || kdbType == KdbType.ANY ? null : kdbType.getTypeName();
    }

    private boolean isScientificNotation(double s) {
        return options.isScientificNotation() && !Double.isInfinite(s) && !Double.isNaN(s) && ((s > 0 && s <= 0.00001) || (s >= -0.00001 && s < 0) || s <= -10000000 || s >= 10000000);
    }
}
package org.kdb.inside.brains.view.console;

import kx.c;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.view.FormatterOptions;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KdbOutputFormatterTest {
    private FormatterOptions options;

    private ConsoleOptions consoleOptions;
    private NumericalOptions numericalOptions;

    @BeforeEach
    void init() {
        consoleOptions = new ConsoleOptions();
        consoleOptions.setEnlistArrays(false);

        numericalOptions = new NumericalOptions();

        options = new FormatterOptions(consoleOptions, numericalOptions);
    }

    @Test
    void nill() {
        assertEquals("(::)", convert(null));
    }

    @Test
    void symbol() {
        assertEquals("`symbol$()", convert(new String[0]));

        assertEquals("`asd", convert("asd"));
        assertEquals(",`asd", convert(new String[]{"asd"}));
        assertEquals("`asd`qwe", convert(new String[]{"asd", "qwe"}));
    }

    @Test
    void chars() {
        assertEquals("\"a\"", convert('a'));
        assertEquals("\" \"", convert(' '));
        assertEquals(",\"a\"", convert("a".toCharArray()));
        assertEquals("\"\"", convert(new char[0]));
        assertEquals("\"adqweqeqwe\"", convert("adqweqeqwe".toCharArray()));
    }

    @Test
    void bools() {
        assertEquals("1b", convert(true));
        assertEquals("0b", convert(false));
        assertEquals("`boolean$()", convert(new boolean[0]));
        assertEquals(",1b", convert(new boolean[]{true}));
        assertEquals("10101b", convert(new boolean[]{true, false, true, false, true}));
    }

    @Test
    void bytes() {
        assertEquals("0x18", convert((byte) 24));
        assertEquals("`byte$()", convert(new byte[0]));
        assertEquals(",0x18", convert(new byte[]{24}));
        assertEquals("0x182D797D85", convert(new byte[]{24, 45, 121, 125, -123}));
    }

    @Test
    void shorts() {
        assertEquals("24h", convert((short) 24));
        assertEquals(",24h", convert(new short[]{24}));
        assertEquals("`short$()", convert(new short[0]));
        assertEquals("24 45 121 125 -123h", convert(new short[]{24, 45, 121, 125, -123}));
    }

    @Test
    void ints() {
        assertEquals("24i", convert(24));
        assertEquals(",24i", convert(new int[]{24}));
        assertEquals("`int$()", convert(new int[0]));
        assertEquals("24 45 121 125 -123i", convert(new int[]{24, 45, 121, 125, -123}));
    }

    @Test
    void longs() {
        assertEquals("24", convert(24L));
        assertEquals(",24", convert(new long[]{24}));
        assertEquals("`long$()", convert(new long[0]));
        assertEquals("24 45 121 125 -123", convert(new long[]{24, 45, 121, 125, -123}));
    }

    @Test
    void floats() {
        assertEquals("24e", convert(24f));
        assertEquals("24.1000004e", convert(24.1f));
        assertEquals(",24e", convert(new float[]{24}));
        assertEquals(",24.1000004e", convert(new float[]{24.1f}));
        assertEquals("`real$()", convert(new float[0]));
        assertEquals("24 45.1230011 121 125 -123e", convert(new float[]{24, 45.123f, 121, 125, -123}));
    }

    @Test
    void doubles() {
        assertEquals("24f", convert(24d));
        assertEquals("24.234", convert(24.234d));
        assertEquals(",24f", convert(new double[]{24}));
        assertEquals(",24.234", convert(new double[]{24.234}));
        assertEquals("`float$()", convert(new double[0]));
        assertEquals("24 45 121 125 -123f", convert(new double[]{24, 45, 121, 125, -123}));
        assertEquals("24.234 45 121 125 -123", convert(new double[]{24.234, 45, 121, 125, -123}));
    }

    @Test
    void uuids() {
        final UUID u1 = UUID.fromString("8c6b8b64-6815-6084-0a3e-178401251b68");
        final UUID u2 = UUID.fromString("5ae7962d-49f2-404d-5aec-f7c8abbae288");

        assertEquals("8c6b8b64-6815-6084-0a3e-178401251b68", convert(u1));
        assertEquals("`guid$()", convert(new UUID[0]));
        assertEquals(",8c6b8b64-6815-6084-0a3e-178401251b68", convert(new UUID[]{u1}));
        assertEquals("8c6b8b64-6815-6084-0a3e-178401251b68 5ae7962d-49f2-404d-5aec-f7c8abbae288", convert(new UUID[]{u1, u2}));
    }

    @Test
    void dates() {
        final Date d1 = new Date(534523452345L);
        final Date d2 = new Date(7456868725345L);

        assertEquals("1986.12.09", convert(d1));
        assertEquals("`date$()", convert(new Date[0]));
        assertEquals(",1986.12.09", convert(new Date[]{d1}));
        assertEquals("1986.12.09 2206.04.20", convert(new Date[]{d1, d2}));
    }

    @Test
    void times() {
        final Time t1 = new Time(534523452345L);
        final Time t2 = new Time(7456868725345L);

        assertEquals("14:44:12.345", convert(t1));
        assertEquals("`time$()", convert(new Time[0]));
        assertEquals(",14:44:12.345", convert(new Time[]{t1}));
        assertEquals("14:44:12.345 08:25:25.345", convert(new Time[]{t1, t2}));
    }

    @Test
    void utilDates() {
        final java.util.Date t1 = new java.util.Date(534523452345L);
        final java.util.Date t2 = new java.util.Date(7456868725345L);

        assertEquals("1986.12.09T14:44:12.345", convert(t1));
        assertEquals("`datetime$()", convert(new java.util.Date[0]));
        assertEquals(",1986.12.09T14:44:12.345", convert(new java.util.Date[]{t1}));
        assertEquals("1986.12.09T14:44:12.345 2206.04.20T08:25:25.345", convert(new java.util.Date[]{t1, t2}));
    }

    @Test
    void timespan() {
        final c.Timespan t1 = new c.Timespan(534523452345L);
        final c.Timespan t2 = new c.Timespan(7456868725345000L);

        assertEquals("0D00:08:54.523452345", convert(t1));
        assertEquals("`timespan$()", convert(new c.Timespan[0]));
        assertEquals(",0D00:08:54.523452345", convert(new c.Timespan[]{t1}));
        assertEquals("0D00:08:54.523452345 86D07:21:08.725345000", convert(new c.Timespan[]{t1, t2}));
    }

    @Test
    void month() {
        final c.Month t1 = new c.Month(12);
        final c.Month t2 = new c.Month(2442);

        assertEquals("2001.01m", convert(t1));
        assertEquals("`month$()", convert(new c.Month[0]));
        assertEquals(",2001.01m", convert(new c.Month[]{t1}));
        assertEquals("2001.01m 2203.07m", convert(new c.Month[]{t1, t2}));
    }

    @Test
    void minute() {
        final c.Minute t1 = new c.Minute(1244);
        final c.Minute t2 = new c.Minute(5321);

        assertEquals("20:44", convert(t1));
        assertEquals("`minute$()", convert(new c.Minute[0]));
        assertEquals(",20:44", convert(new c.Minute[]{t1}));
        assertEquals("20:44 88:41", convert(new c.Minute[]{t1, t2}));
    }

    @Test
    void second() {
        final c.Second t1 = new c.Second(34523);
        final c.Second t2 = new c.Second(654234);

        assertEquals("09:35:23", convert(t1));
        assertEquals("`second$()", convert(new c.Second[0]));
        assertEquals(",09:35:23", convert(new c.Second[]{t1}));
        assertEquals("09:35:23 181:43:54", convert(new c.Second[]{t1, t2}));
    }

    @Test
    void array() {
        assertEquals(
                "((::);`asd;\"a\";\"qwe\";1b;0b;0x18;25h;26i;37;1.23e;32.123;8c6b8b64-6815-6084-0a3e-178401251b68;1986.12.09;14:44:12.345;1986.12.09T14:44:12.345;0D00:08:54.523452345;2001.01m;20:44;09:35:23)",

                convert(new Object[]{
                        null,
                        "asd",
                        'a',
                        "qwe".toCharArray(),
                        true,
                        false,
                        (byte) 24,
                        (short) 25,
                        26,
                        (long) 37,
                        1.23f,
                        32.123d,
                        UUID.fromString("8c6b8b64-6815-6084-0a3e-178401251b68"),
                        new Date(534523452345L),
                        new Time(534523452345L),
                        new java.util.Date(534523452345L),
                        new c.Timespan(534523452345L),
                        new c.Month(12),
                        new c.Minute(1244),
                        new c.Second(34523)
                }));
    }

    @Test
    void timestamps() {
        final Timestamp v3 = new Timestamp(0L);
        assertEquals("1970.01.01D00:00:00.000000000", convert(v3));

        final Timestamp v1 = new Timestamp(1614544496000L);
        v1.setNanos(987654321);
        assertEquals("2021.02.28D20:34:56.987654321", convert(v1));

        final Timestamp v2 = new Timestamp(1614544497000L);
        v2.setNanos(123456789);
        assertEquals("2021.02.28D20:34:56.987654321 2021.02.28D20:34:57.123456789 1970.01.01D00:00:00.000000000", convert(new Timestamp[]{v1, v2, v3}));
    }

    @Test
    void flip() {
        final c.Flip v1 = new c.Flip(new c.Dict(new String[]{"a", "b", "c"},
                new Object[]{
                        new Timestamp[]{new Timestamp(10), new Timestamp(20)},
                        new String[]{"sad", "zxxzcv"},
                        new Object[]{"cbvxcb".toCharArray(), "cvbxcvbncvb".toCharArray()}
                }));
        assertEquals("2#([]a:`timestamp$(); b:`symbol$(); c:())", convert(v1));
    }

    @Test
    void dict_str() {
        final c.Dict v1 = new c.Dict(new String[]{"a", "b", "c"}, new String[]{"aasd", "qweqwe", "fhgfg"});
        assertEquals("`a`b`c!`aasd`qweqwe`fhgfg", convert(v1));
    }

    @Test
    void dict_int() {
        final c.Dict v1 = new c.Dict(new long[]{10, 20, 30}, new int[]{50, 60, 70});
        assertEquals("(10 20 30)!(50 60 70i)", convert(v1));
    }

    @Test
    void dict_mix() {
        final c.Dict v1 = new c.Dict(new String[]{"a", "b", "c"},
                new Object[]{
                        new Timestamp(10),
                        "sad",
                        "cbvxcb"
                });
        assertEquals("`a`b`c!(1970.01.01D00:00:00.010000000;`sad;`cbvxcb)", convert(v1));
    }

    @Test
    void nulls() {
        assertEquals("((::);0b;0Ng;(::);0x00;0Nh;0Ni;0N;0Ne;0n;\" \";`;0Np;0Nm;0Nd;0Nz;0Nn;0Nu;0Nv;0Nt)", convert(c.NULL));

        Object[] o = new Object[]{
                new Object[]{null, null},
                new boolean[]{false, false},
                new byte[]{0, 0},
                new short[]{Short.MIN_VALUE, Short.MIN_VALUE},
                new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE},
                new long[]{Long.MIN_VALUE, Long.MIN_VALUE},
                new float[]{Float.NaN, Float.NaN},
                new double[]{Double.NaN, Double.NaN},
                new Timestamp[]{(Timestamp) c.NULL('p'), (Timestamp) c.NULL('p')},
                new c.Month[]{(c.Month) c.NULL('m'), (c.Month) c.NULL('m')},
                new Date[]{(Date) c.NULL('d'), (Date) c.NULL('d')},
                new java.util.Date[]{(java.util.Date) c.NULL('z'), (java.util.Date) c.NULL('z')},
                new c.Timespan[]{(c.Timespan) c.NULL('n'), (c.Timespan) c.NULL('n')},
                new c.Minute[]{(c.Minute) c.NULL('u'), (c.Minute) c.NULL('u')},
                new c.Second[]{(c.Second) c.NULL('v'), (c.Second) c.NULL('v')},
                new Time[]{(Time) c.NULL('t'), (Time) c.NULL('t')}
        };

        assertEquals("(((::);(::));00b;0x0000;0N 0Nh;0N 0Ni;0N 0N;0N 0Ne;0n 0n;0Np 0Np;0Nm 0Nm;0Nd 0Nd;0Nz 0Nz;0Nn 0Nn;0Nu 0Nu;0Nv 0Nv;0Nt 0Nt)", convert(o));
    }

    @Test
    void precision() {
        numericalOptions.setScientificNotation(false);
        assertThrows(IllegalArgumentException.class, () -> numericalOptions.setFloatPrecision(-1));
        assertThrows(IllegalArgumentException.class, () -> numericalOptions.setFloatPrecision(NumericalOptions.MAX_DECIMAL_PRECISION + 1));

        final double d = 1.1234567891234567891;
        final String s = new BigDecimal(d).toPlainString();
        numericalOptions.setRoundingMode(RoundingMode.DOWN);
        for (int i = 0; i <= NumericalOptions.MAX_DECIMAL_PRECISION; i++) {
            numericalOptions.setFloatPrecision(i);
            numericalOptions.setScientificNotation(false);
            assertEquals(s.substring(0, i + 2), convert(d));

            numericalOptions.setScientificNotation(true);
            assertEquals(s.substring(0, i + 2), convert(d));
        }
    }

    @Test
    void scientists() {
        numericalOptions.setScientificNotation(true);
        assertEquals("0n", convert(Double.NaN));
        assertEquals("-\u221E", convert(Double.NEGATIVE_INFINITY));
        assertEquals("\u221E", convert(Double.POSITIVE_INFINITY));

        assertEquals("0f", convert(0.));
        assertEquals("0.1", convert(0.1));
        assertEquals("0.0001", convert(0.0001));
        assertEquals("1e-005", convert(0.00001));
        assertEquals("1f", convert(1.));
        assertEquals("1000000f", convert(1000000.));
        assertEquals("1e+007", convert(10000000.));
        assertEquals("1e+008", convert(100000000.));
        assertEquals("1e+009", convert(1000000000.));
        assertEquals("-0.1", convert(-0.1));
        assertEquals("-0.0001", convert(-0.0001));
        assertEquals("-1e-005", convert(-0.00001));
        assertEquals("-1f", convert(-1.));
        assertEquals("-1000000f", convert(-1000000.));
        assertEquals("-1e+007", convert(-10000000.));
        assertEquals("-1e+008", convert(-100000000.));
        assertEquals("-1e+009", convert(-1000000000.));

        numericalOptions.setFloatPrecision(15);
        assertEquals("2.567575757567641e+014", convert(256757575756764.1234567890987654321));

        numericalOptions.setFloatPrecision(2);
        assertEquals("2.57e+014", convert(256757575756764.1234567890987654321));

        numericalOptions.setFloatPrecision(0);
        assertEquals("3.e+014", convert(256757575756764.1234567890987654321));
    }

    @Test
    void thousands() {
        assertEquals("1", convert(1L));
        assertEquals("1000", convert(1000L));
        assertEquals("1000000.123", convert(1000000.123));

        options = options.withThousandsSeparator(() -> true);
        assertEquals("1", convert(1L));
        assertEquals("1,000", convert(1000L));
        assertEquals("1,000,000.123", convert(1000000.123));
    }

    @Test
    void rounding() {
        numericalOptions.setFloatPrecision(0);
        final double[] in = new double[]{5.5, 2.5, 1.6, 1.1, 1.0, -1.0, -1.1, -1.6, -2.5, -5.5};

        numericalOptions.setRoundingMode(RoundingMode.UP);
        assertEquals("6. 3. 2. 2. 1. -1. -2. -2. -3. -6.", convert(in));

        numericalOptions.setRoundingMode(RoundingMode.DOWN);
        assertEquals("5. 2. 1. 1. 1. -1. -1. -1. -2. -5.", convert(in));

        numericalOptions.setRoundingMode(RoundingMode.CEILING);
        assertEquals("6. 3. 2. 2. 1. -1. -1. -1. -2. -5.", convert(in));

        numericalOptions.setRoundingMode(RoundingMode.FLOOR);
        assertEquals("5. 2. 1. 1. 1. -1. -2. -2. -3. -6.", convert(in));

        numericalOptions.setRoundingMode(RoundingMode.HALF_UP);
        assertEquals("6. 3. 2. 1. 1. -1. -1. -2. -3. -6.", convert(in));

        numericalOptions.setRoundingMode(RoundingMode.HALF_DOWN);
        assertEquals("5. 2. 2. 1. 1. -1. -1. -2. -2. -5.", convert(in));

        numericalOptions.setRoundingMode(RoundingMode.HALF_EVEN);
        assertEquals("6. 2. 2. 1. 1. -1. -1. -2. -2. -6.", convert(in));
    }

    private String convert(Object o) {
        return new KdbOutputFormatter(options).objectToString(o);
    }
}
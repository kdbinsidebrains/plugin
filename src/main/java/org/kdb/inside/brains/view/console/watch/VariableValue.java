package org.kdb.inside.brains.view.console.watch;

import kx.c;

import java.util.Arrays;

public record VariableValue(boolean valid, Object value) {
    public static boolean changed(VariableValue a, VariableValue b) {
        if (a == b) {
            return false;
        }
        if (a == null || b == null || a.valid != b.valid) {
            return true;
        }
        return !deepEquals(a.value, b.value);
    }

    // Copy of Arrays#deepEquals
    private static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return deepEquals0(a, b);
        }
    }

    // Copy of Arrays#deepEquals0 but adds c.Flip and c.Dict into the look
    private static boolean deepEquals0(Object e1, Object e2) {
        assert e1 != null;
        if (e1 instanceof Object[] && e2 instanceof Object[])
            return deepArraysEquals((Object[]) e1, (Object[]) e2);
        else if (e1 instanceof byte[] && e2 instanceof byte[])
            return Arrays.equals((byte[]) e1, (byte[]) e2);
        else if (e1 instanceof short[] && e2 instanceof short[])
            return Arrays.equals((short[]) e1, (short[]) e2);
        else if (e1 instanceof int[] && e2 instanceof int[])
            return Arrays.equals((int[]) e1, (int[]) e2);
        else if (e1 instanceof long[] && e2 instanceof long[])
            return Arrays.equals((long[]) e1, (long[]) e2);
        else if (e1 instanceof char[] && e2 instanceof char[])
            return Arrays.equals((char[]) e1, (char[]) e2);
        else if (e1 instanceof float[] && e2 instanceof float[])
            return Arrays.equals((float[]) e1, (float[]) e2);
        else if (e1 instanceof double[] && e2 instanceof double[])
            return Arrays.equals((double[]) e1, (double[]) e2);
        else if (e1 instanceof boolean[] && e2 instanceof boolean[])
            return Arrays.equals((boolean[]) e1, (boolean[]) e2);
        else if (e1 instanceof c.Flip f1 && e2 instanceof c.Flip f2)
            return flipEquals(f1, f2);
        else if (e1 instanceof c.Dict f1 && e2 instanceof c.Dict f2)
            return dictEquals(f1, f2);
        return e1.equals(e2);
    }

    // Copy of Arrays#deepEquals
    private static boolean deepArraysEquals(Object[] a1, Object[] a2) {
        if (a1 == a2)
            return true;
        if (a1 == null || a2 == null)
            return false;

        int length = a1.length;
        if (a2.length != length)
            return false;

        for (int i = 0; i < length; i++) {
            Object e1 = a1[i];
            Object e2 = a2[i];

            if (e1 == e2)
                continue;
            if (e1 == null)
                return false;

            // Figure out whether the two elements are equal
            if (!deepEquals0(e1, e2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean dictEquals(c.Dict d1, c.Dict d2) {
        return deepEquals(d1.x, d2.x) && deepEquals(d1.y, d2.y);
    }

    private static boolean flipEquals(c.Flip v1, c.Flip v2) {
        if (!Arrays.equals(v1.x, v2.x)) {
            return false;
        }
        final Object[] y1 = v1.y;
        final Object[] y2 = v2.y;
        if (y1.length != y2.length) {
            return false;
        }
        for (int i = 0; i < y1.length; i++) {
            if (!deepEquals(y1[i], y2[i])) {
                return false;
            }
        }
        return true;
    }
}
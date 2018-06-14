package ru.nettrash.util;

import android.support.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import java.util.*;

/**
 * Created by nettrash on 11.01.2018.
 */

public final class Arrays {

    @Contract(pure = true)
    public static byte[] toByteArray(int[] source) {
        byte[] retVal = new byte[source.length];
        for (int i=0; i<source.length; i++) {
            retVal[i] = source[i] < 128 ? (byte)source[i] : (byte)(source[i] - 256);
        }
        return retVal;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @NonNull
    public static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for(byte b: data)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Contract(pure = true)
    public static int[] toUnsignedByteArray(byte[] source) {
        int[] retVal = new int[source.length];
        for (int i=0; i<source.length; i++) {
            retVal[i] = source[i] & 0xff;
        }
        return retVal;
    }

    public static int[] append(int[] source, int value) {
        source = java.util.Arrays.copyOf(source, source.length+1);
        source[source.length-1] = value;
        return source;
    }

    public static int[] append(int[] source, int[] value) {
        return append(source, value, 0, value.length);
    }

    public static int[] append(int[] source, int[] value, int offset, int size) {
        source = java.util.Arrays.copyOf(source, source.length+size);
        System.arraycopy(value, offset, source, source.length-size, size);
        return source;
    }

    public static byte[] subarray(byte[] source, int offset, int size) {
        byte[] retVal = new byte[size];
        System.arraycopy(source, offset, retVal, 0, size);
        return retVal;
    }

    public static int[] subarray(int[] source, int offset, int size) {
        int[] retVal = new int[size];
        System.arraycopy(source, offset, retVal, 0, size);
        return retVal;
    }

    @Contract(pure = true)
    public static byte[] reverse(byte[] source) {
        byte[] retVal = new byte[source.length];
        for (int i=source.length-1; i>=0; i--) {
            retVal[source.length-1-i] = source[i];
        }
        return retVal;
    }

    @Contract(pure = true)
    public static int[] reverse(int[] source) {
        int[] retVal = new int[source.length];
        for (int i=source.length-1; i>=0; i--) {
            retVal[source.length-1-i] = source[i];
        }
        return retVal;
    }

    public static int[] shifted(int[] source, int shiftAmount) {
        if (source.length > 0 && (shiftAmount % source.length) != 0) {
            int moduloShiftAmount = shiftAmount % source.length;
            boolean negativeShift = shiftAmount < 0;
            int effectiveShiftAmount = negativeShift ? moduloShiftAmount + source.length : moduloShiftAmount;
            Integer[] a = new Integer[source.length];
            int idx = 0;
            for (int i: source) { a[idx++] = Integer.valueOf(i); }
            java.util.Arrays.sort(a, new shiftCompare(effectiveShiftAmount, source.length));
            idx = 0;
            int[] retVal = new int[source.length];
            for (Integer i: a) { retVal[idx++] = i.intValue(); }
            return retVal;
        } else {
            return source;
        }
    }

    public static boolean contains(String[] array, String item) {
        for (String s: array) {
            if (s.equals(item)) return true;
        }
        return false;
    }

    public static boolean containsIgnoreCase(String[] array, String item) {
        for (String s: array) {
            if (s.equalsIgnoreCase(item)) return true;
        }
        return false;
    }
}

final class shiftCompare implements Comparator<Integer> {

    private int shiftAmount;
    private int sourceCount;

    public shiftCompare(int nEffectiveShiftAmount, int nSourceCount) {
        shiftAmount = nEffectiveShiftAmount;
        sourceCount = nSourceCount;
    }

    @Contract(pure = true)
    private int shift(int n) {
        return n + shiftAmount >= sourceCount ? n + shiftAmount - sourceCount : n + shiftAmount;
    }

    @Contract(pure = true)
    public int compare(Integer a, Integer b) {
        return shift(a.intValue()) - shift(b.intValue());
    }
}

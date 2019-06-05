package com.mewna.renderer.utils;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * @author amy
 * @since 6/3/18.
 */
public final class Numbers {
    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();
    
    static {
        suffixes.put(1_000L, "K");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }
    
    private Numbers() {
    }
    
    /**
     * https://stackoverflow.com/questions/4753251/how-to-go-about-formatting-1200-to-1-2k-in-java
     */
    public static String format(final long value) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if(value == Long.MIN_VALUE) {
            //noinspection TailRecursion
            return format(Long.MIN_VALUE + 1);
        }
        if(value < 0) {
            return '-' + format(-value);
        }
        if(value < 1000) {
            return Long.toString(value); //deal with easy case
        }
        
        final Entry<Long, String> e = suffixes.floorEntry(value);
        final Long divideBy = e.getKey();
        final String suffix = e.getValue();
        
        final long truncated = value / (divideBy / 10); //the number part of the output times 10
        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
        final boolean hasDecimal = truncated < 100 && truncated / 10D != truncated / 10;
        return hasDecimal ? truncated / 10D + suffix : truncated / 10 + suffix;
    }
    
    public static String formatBD(final BigDecimal value) {
        final String[] suffixes = {null, "K", "M", "G", "T", "P", "E", "Z", "Y"};
        final BigDecimal thousand = new BigDecimal("1000");
        
        BigDecimal finalValue = value;
        BigDecimal remainder = null;
        int divisions = 0;
        
        BigDecimal[] division;
        while(finalValue.compareTo(thousand) >= 0) {
            division = finalValue.divideAndRemainder(thousand);
            finalValue = division[0];
            remainder = division[1];
            divisions++;
        }
        if(divisions == 0) {
            return value.toString();
        }
        
        try {
            final String suffix = suffixes[divisions];
            return finalValue + "." + String.format("%03d", remainder.intValue()) + suffix;
        } catch(final ArrayIndexOutOfBoundsException ignored) {
            return finalValue.toString();
        }
    }
}

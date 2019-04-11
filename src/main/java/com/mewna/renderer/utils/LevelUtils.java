package com.mewna.renderer.utils;

/**
 * @author amy
 * @since 4/10/19.
 */
public class LevelUtils {
    public static long levelToXp(final long level) {
        return Math.max(0, 100 * level + 20 * (level - 1));
    }
    
    public static long fullLevelToXp(final long level) {
        long requiredXp = 0;
        for(int i = 1; i <= level; i++) {
            requiredXp += levelToXp(i);
        }
        return requiredXp;
    }
    
    public static long nextLevelXp(final long xp) {
        return fullLevelToXp(xpToLevel(xp) + 1) - xp;
    }
    
    public static long xpToLevel(long xp) {
        long level = 0;
        while(xp >= levelToXp(level)) {
            xp -= levelToXp(level);
            level += 1;
        }
        
        return Math.max(0, level - 1);
    }
}

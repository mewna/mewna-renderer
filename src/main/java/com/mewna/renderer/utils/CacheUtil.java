package com.mewna.renderer.utils;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author amy
 * @since 6/3/18.
 */
@SuppressWarnings("unused")
public final class CacheUtil {
    private static final Map<String, CachedImage> IMAGE_CACHE = new ConcurrentHashMap<>();
    
    private CacheUtil() {
    }
    
    /**
     * Caches and returns an image from inside the local JAR.
     *
     * @param path Path to the image resource inside the JAR
     *
     * @return The cached image resource
     */
    public static CachedImage getImageResource(final String path) {
        if(IMAGE_CACHE.containsKey(path)) {
            return IMAGE_CACHE.get(path);
        }
        try(final InputStream is = CacheUtil.class.getResourceAsStream(path)) {
            final BufferedImage image = ImageIO.read(is);
            final CachedImage cachedImage = new CachedImage(System.currentTimeMillis(), path, image);
            IMAGE_CACHE.put(path, cachedImage);
            return cachedImage;
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new RuntimeException(e);
        }
    }
    
    @Value
    @RequiredArgsConstructor
    public static final class CachedImage {
        private final long cachedAt;
        private final String path;
        private final BufferedImage image;
    }
}

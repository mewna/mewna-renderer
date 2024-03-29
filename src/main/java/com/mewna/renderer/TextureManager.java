package com.mewna.renderer;

import com.mewna.renderer.utils.CacheUtil;
import com.mewna.renderer.utils.IOUtils;
import io.sentry.Sentry;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Manages textures. duh.
 * <p>
 * Things like local image assets are stored locally, ie per-process so that we
 * can easily roll updates without having to worry about expiring caches.
 * Things like avatars are cached in redis, so that we can reduce memory usage
 * and share a cache between all backend workers.
 *
 * @author amy
 * @since 6/4/18.
 */
@SuppressWarnings("WeakerAccess")
public final class TextureManager {
    @SuppressWarnings("TypeMayBeWeakened")
    private static final List<Background> BACKGROUNDS = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(TextureManager.class);
    private static final String AVATAR_CACHE_KEY = "cache:%s:avatar";
    @SuppressWarnings({"StaticVariableOfConcreteClass", "WeakerAccess", "PublicField"})
    public static Background defaultBg;
    private static boolean preloaded;
    
    private TextureManager() {
    }
    
    public static Optional<Background> getBackground(final String background) {
        return BACKGROUNDS.stream()
                .filter(e -> e.path.equalsIgnoreCase(background + ".png"))
                .findFirst();
    }
    
    public static void preload() {
        if(preloaded) {
            return;
        }
        preloaded = true;
        IOUtils.scan("backgrounds", e -> {
            if(!e.isDirectory() && e.getName().toLowerCase().endsWith(".png") && !e.getName().contains("thumbs")) {
                final String path = '/' + e.getName();
                final Background bg = new Background(path);
                BACKGROUNDS.add(bg);
                if(path.toLowerCase().endsWith("plasma.png")) {
                    defaultBg = bg;
                }
                LOGGER.info("Cached: {} (pack {}, name {})", CacheUtil.getImageResource(path).getPath(), bg.pack, bg.name);
            }
        });
    }
    
    private static void cacheAvatar(final Renderer renderer, final String id, final String avatarUrl) {
        final BufferedImage avatar = downloadAvatar(avatarUrl);
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(avatar, "png", baos);
            final byte[] bytes = baos.toByteArray();
            baos.close();
            renderer.redis(r -> r.set(String.format(AVATAR_CACHE_KEY, id).getBytes(), bytes));
            expireAvatar(renderer, id);
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    private static void expireAvatar(final Renderer renderer, final String id) {
        // expire in 1 day
        renderer.redis(r -> r.expire(String.format(AVATAR_CACHE_KEY, id), 3600 * 24));
    }
    
    @SuppressWarnings("WeakerAccess")
    public static BufferedImage getCachedAvatar(final Renderer renderer, final String id, final String avatarUrl) {
        final BufferedImage[] avatar = {null};
        
        renderer.redis(r -> {
            if(r.exists(String.format(AVATAR_CACHE_KEY, id))) {
                // Exists, return it
                try {
                    final byte[] bytes = r.get(String.format(AVATAR_CACHE_KEY, id).getBytes());
                    final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    avatar[0] = ImageIO.read(bais);
                    bais.close();
                } catch(final IOException e) {
                    Sentry.capture(e);
                    throw new IllegalStateException(e);
                }
            } else {
                // Doesn't exist, cache it
                cacheAvatar(renderer, id, avatarUrl);
                avatar[0] = getCachedAvatar(renderer, id, avatarUrl);
            }
        });
        
        return avatar[0];
    }
    
    private static BufferedImage downloadAvatar(final String avatarUrl) {
        try {
            final URL url = new URL(avatarUrl);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // Default Java user agent gets blocked by Discord :(
            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/55.0.2883.87 Safari/537.36");
            final InputStream is = connection.getInputStream();
            final BufferedImage avatar = ImageIO.read(is);
            is.close();
            connection.disconnect();
            return avatar;
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new RuntimeException(e);
        }
    }
    
    @Getter
    @SuppressWarnings("WeakerAccess")
    public static final class Background {
        private final String name;
        private final String pack;
        private final String path;
        
        private Background(String path) {
            if(!path.startsWith("/")) {
                path = '/' + path;
            }
            this.path = path;
            // String leading /backgrounds
            path = path.replaceFirst("/backgrounds/", "");
            final String[] split = path.split("/", 2);
            name = split[1].replace(".png", "");
            pack = split[0];
        }
    }
}

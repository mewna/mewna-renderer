package com.mewna.renderer;

import com.mewna.renderer.api.API;
import com.mewna.renderer.utils.IOUtils;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import gg.amy.singyeong.SingyeongClient;
import io.sentry.Sentry;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author amy
 * @since 4/10/19.
 */
@Accessors(fluent = true)
public final class Renderer {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private final Vertx vertx = Vertx.vertx();
    @Getter
    private final SingyeongClient singyeong;
    @Getter
    private final StatsDClient statsClient;
    @Getter
    private final int port = Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("80"));
    
    private JedisPool jedisPool;
    
    private Renderer() {
        if(System.getenv("STATSD_ENABLED") != null) {
            statsClient = new NonBlockingStatsDClient("v2.backend", System.getenv("STATSD_HOST"), 8125);
        } else {
            statsClient = new NoOpStatsDClient();
        }
        // Initialized here to avoid breaking things
        singyeong = SingyeongClient.create(vertx, System.getenv("SINGYEONG_DSN"), IOUtils.ip() + ':' + port);
    }
    
    public static void main(final String[] args) {
        new Renderer().start();
    }
    
    private void start() {
        logger.info("Starting card renderer...");
        Sentry.init();
        logger.info("Preloading textures...");
        TextureManager.preload();
        logger.info("Connecting to Redis...");
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(10);
        config.setMaxTotal(100);
        config.setMaxWaitMillis(500);
        jedisPool = new JedisPool(config, System.getenv("REDIS_HOST"));
        new API(this).start();
        logger.info("All ready! Connecting to singyeong...");
        singyeong.connect()
                .thenAccept(__ -> singyeong.onInvalid(i -> logger.info("Singyeong invalid: {}: {}", i.nonce(), i.reason())))
                .thenAccept(__ -> logger.info("Finished starting!"))
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }
    
    @SuppressWarnings("WeakerAccess")
    public void redis(final Consumer<Jedis> c) {
        try(final Jedis jedis = jedisPool.getResource()) {
            jedis.auth(System.getenv("REDIS_PASS"));
            c.accept(jedis);
        }
    }
}

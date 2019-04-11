package com.mewna.renderer.api;

import com.mewna.renderer.CardRenderer;
import com.mewna.renderer.Renderer;
import io.sentry.Sentry;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static com.mewna.renderer.utils.Async.move;

/**
 * @author amy
 * @since 4/10/19.
 */
public class API {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Renderer renderer;
    
    public API(final Renderer renderer) {
        this.renderer = renderer;
    }
    
    public void start() {
        logger.info("Starting API server...");
        final HttpServer server = renderer.vertx().createHttpServer();
        final Router router = Router.router(renderer.vertx());
        router.post("/v1/render/rank").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final var id = UUID.randomUUID();
            try {
                logger.info("[{}] Rendering rank card", id);
                final var rankData = ctx.getBodyAsJson().mapTo(RankData.class);
                final byte[] cardBytes = CardRenderer.generateRankCard(renderer, rankData);
                logger.info("[{}] Rendered {} bytes for rank card", id, cardBytes.length);
                ctx.response()
                        .putHeader("Content-Length", String.valueOf(cardBytes.length))
                        .write(Buffer.buffer(cardBytes))
                        .end();
            } catch(final Exception e) {
                e.printStackTrace();
                Sentry.capture(e);
                if(!ctx.response().closed()) {
                    ctx.response().end(new JsonObject().put("status", "error").encode());
                }
            }
        }));
        router.post("/v1/render/profile").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final var id = UUID.randomUUID();
            try {
                logger.info("[{}] Rendering profile card", id);
                final var rankData = ctx.getBodyAsJson().mapTo(ProfileData.class);
                final byte[] cardBytes = CardRenderer.generateProfileCard(renderer, rankData);
                logger.info("[{}] Rendered {} bytes for profile card", id, cardBytes.length);
                ctx.response()
                        .putHeader("Content-Length", String.valueOf(cardBytes.length))
                        .write(Buffer.buffer(cardBytes))
                        .end();
            } catch(final Exception e) {
                e.printStackTrace();
                Sentry.capture(e);
                if(!ctx.response().closed()) {
                    ctx.response().end(new JsonObject().put("status", "error").encode());
                }
            }
        }));
        server.requestHandler(router).listen(renderer.port());
    }
}

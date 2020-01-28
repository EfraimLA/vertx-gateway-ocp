package io.vertx.example.common;

import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.healthchecks.HealthCheckHandler;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;

import java.util.HashSet;
import java.util.Set;

public abstract class RestAPIVerticle extends BaseMicroserviceVerticle {

    protected Router router;
    protected boolean online = false;

    protected Single<HttpServer> createHttpServer(final int port, final String host) {
        return vertx.createHttpServer()
                .requestHandler(router)
                .rxListen(port, host);
    }

    protected Single<HttpServer> createHttpServer(final int port) {
        return vertx.createHttpServer()
                .requestHandler(router)
                .rxListen(port);
    }

    protected void enableCorsSupport(final Router router) {
        final Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("origin");
        allowHeaders.add("Content-Type");
        allowHeaders.add("accept");

        router.route().handler(CorsHandler.create(config().getString("cors-allow", "*"))
                .allowedHeaders(allowHeaders)
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.PATCH)
                .allowedMethod(HttpMethod.OPTIONS)
        );
    }

    protected void configureRouter() {
        router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
//        router.route().handler(LoggerHandler.create(true, LoggerHandler.DEFAULT_FORMAT));
    }

    protected void enableHealthCheck(final Router router) {
        final HealthCheckHandler hc = HealthCheckHandler.create(vertx);

        hc.register("server-online", promise -> {
            if (online) promise.complete();
            else promise.fail("Not Alive");
        });

        router.get("/health/readiness").handler(rc -> rc.response().end());
        router.get("/health/liveness").handler(hc);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}

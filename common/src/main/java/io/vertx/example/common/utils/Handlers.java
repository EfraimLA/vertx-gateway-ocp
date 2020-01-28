package io.vertx.example.common.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

public final class Handlers {

    public static final String CONT = "content-type";
    public static final String TYPE = "application/json";

    public static void handleNotFound(final RoutingContext rc) {
        rc.response().setStatusCode(404).end();
    }

    public static void handleResponse(final RoutingContext rc, final JsonObject res) {
        rc.response().putHeader(CONT, TYPE).end(res.encodePrettily());
    }

    public static void handleBadGateway(final RoutingContext rc) {
        rc.response().setStatusCode(502).putHeader(CONT, TYPE)
                .end(new JsonObject().put("message", "Bad Gateway").encodePrettily());
    }
}

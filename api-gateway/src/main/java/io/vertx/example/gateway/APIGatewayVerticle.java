package io.vertx.example.gateway;

import io.vertx.example.common.RestAPIVerticle;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.HttpEndpoint;

import static io.vertx.example.common.utils.Handlers.*;

public class APIGatewayVerticle extends RestAPIVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(APIGatewayVerticle.class);

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting APIGatewayVerticle...");
        super.start();

        configureRouter();
        enableCorsSupport(router);
        enableHealthCheck(router);

        configureRoutes();

        final int port = config().getInteger("http.port", 8080);
        createHttpServer(port)
                .subscribe(result -> {
                    online = true;
                    LOGGER.info("Server started");
                }, Throwable::printStackTrace);
    }

    private void configureRoutes() {
        router.get("/api/v").handler(this::apiVersion);

        router.route("/api/*").handler(this::dispatchRequest);
    }

    private void dispatchRequest(RoutingContext rc) {
        int initialOffset = 5; // Length of `/api/`

        circuitBreaker.rxExecute(promise -> {
            String path = rc.request().uri();

            if (path.length() <= initialOffset) {
                handleNotFound(rc);
                promise.complete();
                return;
            }

            String prefix = (path.substring(initialOffset).split("/"))[0];

            LOGGER.info("Prefix: " + prefix);

            String newPath = path.substring(initialOffset + prefix.length());

            LOGGER.info("New path: " + newPath);

            HttpEndpoint.rxGetWebClient(discovery, record -> record.getMetadata().getString("api.name").equals(prefix))
                    .subscribe(webClient -> {
                        LOGGER.info("Fetched Service!");
                        doDispatch(rc, newPath, webClient, promise);
                    }, t -> {
                        LOGGER.info("Throwable: ", t.getMessage());
                        handleNotFound(rc);
                        promise.complete();
                    });
        }).subscribe(o -> LOGGER.info("Circuit breaker succeed"), t -> {
            t.printStackTrace();
            handleBadGateway(rc);
        });
    }

    private void doDispatch(RoutingContext rc, String path, WebClient client, Promise<Object> cbPromise) {
        LOGGER.info("Processing request...");

        HttpRequest<Buffer> httpRequest = client.request(rc.request().method(), path);

        rc.request().headers().forEach(header -> httpRequest.putHeader(header.getKey(), header.getValue()));

        Single<HttpResponse<Buffer>> httpResponse;

        if (rc.getBody() == null) {
            httpResponse = httpRequest.rxSend();
        } else {
            httpResponse = httpRequest.rxSendBuffer(rc.getBody());
        }

        httpResponse.subscribe(res -> {
            if (res.statusCode() == 500) {
                LOGGER.info("Thrown 500 status code");
                cbPromise.fail(String.format("%d : %s", res.statusCode(), res.body().toString()));
            } else {
                LOGGER.info("Sending response...");

                HttpServerResponse response = rc.response().setStatusCode(res.statusCode());

                res.headers().forEach(header -> response.putHeader(header.getKey(), header.getValue()));

                if (res.body() != null) {
                    LOGGER.info("Response body: " + res.bodyAsString());
                    response.end(res.body());
                } else {
                    response.end();
                }

                LOGGER.info("Response headers: " + res.headers().toString());
                LOGGER.info("Response status code: " + res.statusCode());

                cbPromise.complete();
            }

            LOGGER.info("Releasing Service Object...");
            ServiceDiscovery.releaseServiceObject(discovery, client);
        }, cbPromise::fail);
    }

    private void apiVersion(RoutingContext rc) {
        handleResponse(rc, new JsonObject().put("version", "v1"));
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping APIGatewayVerticle...");
        super.stop();
    }
}

package io.vertx.example.microservice;

import io.vertx.example.common.RestAPIVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MicroserviceVerticle extends RestAPIVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceVerticle.class);
    private static final String SERVICE_NAME = "microservice";

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting microservice...");
        super.start();

        configureRouter();
        enableCorsSupport(router);
        enableHealthCheck(router);

        configureRoutes();

        final String host = config().getString("http.host", "0.0.0.0");
        final int port = config().getInteger("http.port", 8080);

        createHttpServer(port, host)
                .subscribe(r -> {
                    online = true;
                    LOGGER.info("Server started");
                    publishHttpEndpoint(SERVICE_NAME, host, port)
                            .subscribe();
                }, Throwable::printStackTrace);
    }

    private void configureRoutes() {
        router.get("/").handler(rc -> rc.response().end("OK!"));

        router.get("/ticket/").handler(rc -> rc.response().end(" invalid ticket"));

        router.get("/ticket/invalid/").handler(rc -> rc.response().end(" invalid ticket"));

        router.get("/employee/").handler(rc -> rc.response().end("employee"));

        router.get("/staff/").handler(rc -> rc.response().end("staff"));
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping microservice ...");
        super.stop();
    }
}

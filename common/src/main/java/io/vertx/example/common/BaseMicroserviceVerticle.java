package io.vertx.example.common;

import io.reactivex.Single;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.circuitbreaker.CircuitBreaker;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;

import java.util.Set;

public abstract class BaseMicroserviceVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMicroserviceVerticle.class);

    protected ServiceDiscovery discovery;
    protected CircuitBreaker circuitBreaker;
    protected final Set<Record> registeredRecords = new ConcurrentHashSet<>();

    @Override
    public void start() throws Exception {
        discovery = ServiceDiscovery.create(vertx);

        final JsonObject cbOptions = config().getJsonObject("circuit-breaker") != null ? config().getJsonObject("circuit-breaker") : new JsonObject();

        circuitBreaker = CircuitBreaker.create(cbOptions.getString("name", "circuit-breaker"), vertx,
                new CircuitBreakerOptions()
                        .setMaxFailures(cbOptions.getInteger("maxFailures", 5))
                        .setTimeout(cbOptions.getLong("timeout", 10000L))
                        .setFallbackOnFailure(true)
                        .setResetTimeout(cbOptions.getLong("resetTimeout", 30000L))
        );
    }

    private Single<Record> publish(final Record record) {
        return discovery.rxPublish(record)
                .doOnSuccess(rec -> {
                    registeredRecords.add(record);
                    LOGGER.info("Service <" + rec.getName() + "> published");
                });
    }


    protected Single<Record> publishHttpEndpoint(final String name, final String host, final int port) {
        final Record record = HttpEndpoint.createRecord(name, host, port, "/",
                new JsonObject().put("api.name", config().getString("api.name", name))
        );

        return publish(record);
    }

    @Override
    public void stop() throws Exception {
        registeredRecords.forEach(record -> discovery.rxUnpublish(record.getRegistration()).subscribe());

        discovery.close();
        circuitBreaker.close();
    }
}

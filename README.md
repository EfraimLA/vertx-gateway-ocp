# vertx-gateway-ocp

You have to be signed into Openshift with Openshift-client (OC)

To deploy project just run:
```
./scripts/deploy-all.sh
```

This will deploy a microservice with some routes and published as HttpEndpoint to ServiceDiscovery with root path "/"

And an API Gateway getting a WebClient from ServiceDiscovery matching the prefix of the route after "/api/", for example: "/api/__microservice__/". This will search the Endpoint microservice in ServiceDiscovery

So fetch the Endpoint and request the route after the prefix, for example: "/api/__microservice__/staff/" will reach the route "/staff/" within microservice endpoint


To stop and delete project run:
```
./scripts/clean-all.sh
```

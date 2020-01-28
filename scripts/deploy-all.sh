#!/usr/bin/env bash
echo "Deploying project"
oc new-project gateway
oc policy add-role-to-user view admin -n gateway
oc policy add-role-to-user view -n gateway -z default
oc policy add-role-to-group view system:serviceaccounts -n gateway
oc policy add-role-to-user view system:serviceaccount:gateway:default -n gateway

oc create configmap api-gateway-config --from-file=api-gateway/src/kubernetes/
oc create configmap microservice-config --from-file=microservice/src/kubernetes/

mvn clean fabric8:deploy

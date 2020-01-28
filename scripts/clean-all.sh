#!/usr/bin/env bash
echo "Cleaning all project"
oc delete project gateway
rm -rf common/target
rm -rf microservice/target
rm -rf api-gateway/target

rm -rf common/src/main/generated
rm -rf microservice/src/main/generated
rm -rf api-gateway/src/main/generated

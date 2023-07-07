#!/bin/sh
read -p "Enter project (eg. wnc, summit, pdd, sgmap):" PROJECT
read -p "Enter vertical (eg. door, planes, weather):" VERTICAL

LOWERCASE_VERTICAL="${VERTICAL,,}"
LOWERCASE_PROJECT="${PROJECT,,}"
LOWERCASE_CONTROLLER="${LOWERCASE_VERTICAL}-controller"

sed -i "s/controllerproject/$LOWERCASE_PROJECT/g" ./src/main/resources/application.properties
sed -i "s/controllerbase/$LOWERCASE_VERTICAL/g" ./src/main/resources/application.properties
sed -i "s/controllerbaseimage/$LOWERCASE_CONTROLLER/g" ./pom.xml
sed -i "s/controllerbase/$LOWERCASE_CONTROLLER/g" ./Dockerfile

sed -i "s/controllerbase/$LOWERCASE_CONTROLLER/g" ./deploy/deployment/controllerbase-app.yaml && mv ./deploy/deployment/controllerbase-app.yaml "./deploy/deployment/$LOWERCASE_CONTROLLER-app.yaml"
sed -i "s/controllerbase/$LOWERCASE_CONTROLLER/g" ./deploy/volumes/controllerbase-app-pv.yaml && mv ./deploy/volumes/controllerbase-app-pv.yaml "./deploy/deployment/$LOWERCASE_CONTROLLER-app-pv.yaml"
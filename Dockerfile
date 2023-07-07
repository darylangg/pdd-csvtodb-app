FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk update && apk add bash
# copy the packaged jar file into our docker image
COPY target/pdd-csvtodb-app*.jar ./pdd-csvtodb-app.jar
COPY target/application.properties ./application.properties

ENV JAVA_OPTS="-XX:PermSize=1024m -XX:MaxPermSize=512m"
# set the startup command to execute the jar
CMD ["java","-jar","pdd-csvtodb-app.jar"]
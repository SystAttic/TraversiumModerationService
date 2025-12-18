FROM eclipse-temurin:17-jdk
MAINTAINER Traversium Developers
WORKDIR /opt/moderation-service

COPY service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/opt/moderation-service/app.jar"]
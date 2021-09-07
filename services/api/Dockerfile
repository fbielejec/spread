FROM openjdk:11.0.6-jre-stretch
MAINTAINER "Filip Bielejec" <fbielejec@gmail.com>

WORKDIR api

COPY api-service.jar /api/api-service.jar

EXPOSE 3001

ENTRYPOINT ["java", "-jar" , "api-service.jar"]
CMD ["--max-old-space-size=2048"]

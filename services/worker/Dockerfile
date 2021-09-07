FROM openjdk:11.0.6-jre-stretch
MAINTAINER "Filip Bielejec" <fbielejec@gmail.com>

WORKDIR worker

COPY worker-service.jar /worker/worker-service.jar

ENTRYPOINT ["java", "-jar" , "worker-service.jar"]
CMD ["--max-old-space-size=2048"]

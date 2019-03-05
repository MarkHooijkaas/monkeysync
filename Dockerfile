FROM gradle:jdk8 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build

FROM openjdk:8-jre-slim
COPY --from=builder /home/gradle/src/build/libs/monkeysync-fat-latest.jar /app/
WORKDIR /app
CMD java -jar monkeysync-fat-latest.jar
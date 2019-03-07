FROM openjdk:8-jre-alpine
COPY build/libs/monkeysync-fat-latest.jar /app/
WORKDIR /app
ENTRYPOINT ["/usr/bin/java", "-jar", "monkeysync-fat-latest.jar"]
CMD ["-c", "-", "--help"]
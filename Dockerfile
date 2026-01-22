# https://careers.wolt.com/en/blog/tech/how-to-reduce-jvm-docker-image-size
FROM contrastsecurity-docker-harmony-releases.jfrog.io/contrast-distroless-java-21-deb13:latest

COPY build/contrast-intellij-plugin*.jar /app/contrast-intellij-plugin.jar

CMD ["/app/contrast-intellij-plugin.jar"]
ENTRYPOINT [ "java", "-jar", "/app/contrast-intellij-plugin.jar" ]

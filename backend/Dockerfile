# ==============================================================================
# Image d'exécution minimale JRE 17 pour Spring Boot 3
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Utilisateur non-root pour des raisons de sécurité
RUN addgroup -S careertrack && adduser -S careertrack -G careertrack

# Copie de l'archive JAR exécutable compilée
COPY target/*.jar app.jar
RUN chown -R careertrack:careertrack /app

USER careertrack

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

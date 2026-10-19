# ==============================================================================
# Étape 1 : Compilation avec Maven et JDK 17
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache des dépendances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copie des sources et compilation
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Étape 2 : Image d'exécution minimale JRE 17
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Utilisateur non-root pour des raisons de sécurité
RUN addgroup -S careertrack && adduser -S careertrack -G careertrack

# Copie de l'archive JAR générée depuis l'étape de compilation
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R careertrack:careertrack /app

USER careertrack

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

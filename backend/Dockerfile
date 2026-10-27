# Imagen del backend para el servidor (EC2). Se construye desde deploy/docker-compose.prod.yml.
# Etapa 1: compila el .jar con el Maven Wrapper del proyecto (no hace falta tener Maven ni Java en el servidor).
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# Las pruebas y el formato ya los valida el CI en cada PR; aquí solo se empaqueta.
RUN ./mvnw -B -q package -DskipTests

# Etapa 2: solo el JRE y el .jar, sin código fuente ni Maven.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 hellofriend
COPY --from=compilacion /app/target/*.jar app.jar
USER hellofriend
# La JVM usa como máximo el 70 % de la memoria que el compose le asigna al contenedor.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

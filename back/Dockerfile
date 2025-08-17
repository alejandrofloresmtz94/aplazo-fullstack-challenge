FROM eclipse-temurin:21-jdk AS base
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn

# Copia el código fuente
COPY src src

# Da permisos de ejecución al wrapper
RUN chmod +x mvnw

# Construye el proyecto y genera el jar
RUN ./mvnw clean package -DskipTests

# Segunda etapa: solo el jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=base /app/target/*.jar app.jar

# Expone el puerto 8080
EXPOSE 8080

# Comando para ejecutar la app
ENTRYPOINT ["java", "-jar", "app.jar"]

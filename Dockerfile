# Stage 1: Build the application using Maven
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy all files and give Maven wrapper permissions
COPY . .
RUN chmod +x mvnw

# Build the Spring Boot app (skipping tests) 
RUN ./mvnw clean package -DskipTests

# --------------------------------------------

# Stage 2: Run the application from a lightweight image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app


# Copy the built JAR from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose port for Spring Boot (default 8080)
EXPOSE 8080

# Run the JAR
CMD ["java", "-jar", "app.jar"]

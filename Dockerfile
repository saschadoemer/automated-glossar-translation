# Use a lightweight JRE 23 image for the final container
# The build process is handled externally; this image only packages the resulting binary.
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

# Create a non-root user and group for security purposes
# Running as a non-privileged user reduces the attack surface
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the generated JAR file from the local target directory
# Using a wildcard to match the JAR file regardless of the version
# It is assumed that the binary is provided before building this Docker image.
COPY target/*.jar app.jar

# Change the ownership of the application files to the non-root user
RUN chown -R spring:spring /app

# Switch to the non-root user for subsequent commands
USER spring

# Expose the default Spring Boot port
EXPOSE 8080

# Configure the entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built jar file
COPY target/temporal-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the app runs on
EXPOSE 8084

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 
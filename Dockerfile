# ---- Build stage: Maven + JDK compile the app and run no tests (CI already ran them) ----
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace
# Copy only the build files first so the dependency download is cached between builds
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---- Runtime stage: slim JRE of the same Java version, no Maven, no source ----
FROM eclipse-temurin:21-jre-noble
WORKDIR /app
# Non-root user so a compromised app cannot act as root in the container
RUN useradd --system --uid 1001 appuser
COPY --from=build /workspace/target/*.jar app.jar
# The commit SHA is baked in at build time; the footer and /api/info read it
ARG GIT_SHA=dev
ENV APP_GIT_SHA=$GIT_SHA
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

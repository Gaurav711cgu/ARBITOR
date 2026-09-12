# Stage 1: Build Frontend (Node.js)
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Backend (Java 21)
FROM eclipse-temurin:21-jdk AS backend-build
WORKDIR /workspace
COPY src ./src
RUN mkdir -p out/classes && javac --release 21 -d out/classes $(find src/main/java -name '*.java' | sort)

# Stage 3: Production Image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy backend classes
COPY --from=backend-build /workspace/out/classes ./out/classes

# Copy frontend dist to web/ (so ArbiterApplication serves it statically)
COPY --from=frontend-build /app/frontend/dist ./web

# Copy benchmark data (if needed by endpoints)
COPY benchmark-results ./benchmark-results

ENV ARBITER_DATA_DIR=/data
EXPOSE 8080
VOLUME ["/data"]

CMD ["sh", "-c", "test -n \"$ARBITER_API_KEY\" || { echo 'ARBITER_API_KEY must be set and at least 24 characters' >&2; exit 1; }; java -Darbiter.data.dir=\"$ARBITER_DATA_DIR\" -Darbiter.api.key=\"$ARBITER_API_KEY\" -cp out/classes com.arbiter.ArbiterApplication"]

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY src ./src
RUN mkdir -p out/classes && javac --release 21 -d out/classes $(find src/main/java -name '*.java' | sort)

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/out/classes ./out/classes
COPY web ./web
COPY benchmark-results ./benchmark-results
ENV ARBITER_DATA_DIR=/data
EXPOSE 8080
VOLUME ["/data"]
CMD ["sh", "-c", "test -n \"$ARBITER_API_KEY\" || { echo 'ARBITER_API_KEY must be set and at least 24 characters' >&2; exit 1; }; java -Darbiter.data.dir=\"$ARBITER_DATA_DIR\" -Darbiter.api.key=\"$ARBITER_API_KEY\" -cp out/classes com.arbiter.ArbiterApplication"]


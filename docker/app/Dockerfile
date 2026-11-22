FROM node:22-alpine AS ui-builder
WORKDIR /workspace/grcone-ui

COPY grcone-ui/package*.json ./
RUN npm ci

COPY grcone-ui/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS app-builder
WORKDIR /workspace

COPY grcone-app/pom.xml grcone-app/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f grcone-app/pom.xml -DskipTests -Dskip.ui=true dependency:go-offline

COPY grcone-app/ grcone-app/
COPY --from=ui-builder /workspace/grcone-ui/dist grcone-ui/dist
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f grcone-app/pom.xml -DskipTests -Dskip.ui=true package
RUN find grcone-app/target -maxdepth 1 -type f -name "grcone-app-*.jar" ! -name "*.original" \
    -exec cp {} /tmp/grcone-app.jar \; \
    && test -f /tmp/grcone-app.jar

FROM eclipse-temurin:21-jre-noble AS runtime
WORKDIR /opt/grcone

ENV APP_JAR=/opt/grcone/grcone-app.jar
ENV SERVER_PORT=8080

COPY docker/app/entrypoint.sh /usr/local/bin/grcone-app-entrypoint.sh
COPY --from=app-builder /tmp/grcone-app.jar /opt/grcone/grcone-app.jar

RUN chmod +x /usr/local/bin/grcone-app-entrypoint.sh; \
    useradd --system --home-dir /opt/grcone --shell /usr/sbin/nologin grcone; \
    chown -R grcone:grcone /opt/grcone

USER grcone
EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/grcone-app-entrypoint.sh"]

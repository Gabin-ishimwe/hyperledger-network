# the first stage
FROM maven:3.9-eclipse-temurin-11 AS maven_build

WORKDIR /build

# copy the pom.xml and src code to the container
COPY pom.xml ./
COPY src/ src/

# Build and package our code
RUN mvn clean package -DskipTests -Dcheckstyle.skip=true


# the second stage of our build just needs the compiled files
FROM eclipse-temurin:11-jre
ARG CC_SERVER_PORT=9999

# Setup tini to work better handle signals - install from package manager for correct architecture
RUN apt-get update && apt-get install -y tini && rm -rf /var/lib/apt/lists/*

RUN addgroup --system javauser && useradd -g javauser javauser

# copy only the artifacts we need from the first stage and discard the rest
COPY --chown=javauser:javauser --from=maven_build /build/target/chaincode.jar /chaincode.jar
COPY --chown=javauser:javauser docker/docker-entrypoint.sh /docker-entrypoint.sh

ENV PORT=$CC_SERVER_PORT
EXPOSE $CC_SERVER_PORT

USER javauser
ENTRYPOINT [ "tini", "--", "/docker-entrypoint.sh" ]

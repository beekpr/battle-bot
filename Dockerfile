FROM openjdk:8-jre

LABEL maintainer="Beekeeper AG <admin@beekeeper.io>"
LABEL Description="Battle Bot"

WORKDIR /opt/bkpr

COPY build/libs/battle-bot-all.jar /opt/bkpr

ENTRYPOINT ["java"]
CMD ["-jar", "/opt/bkpr/battle-bot-all.jar"]
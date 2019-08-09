FROM openjdk:8-jre

LABEL maintainer="Beekeeper AG <admin@beekeeper.io>"
LABEL Description="Battle Bot"

WORKDIR /opt/bkpr

COPY build/libs/battle-bot-all.jar /opt/bkpr
COPY BattleBot-ffe28c6779de.json /opt/bkpr/cred.json

ENTRYPOINT ["java"]
CMD ["-jar", "/opt/bkpr/battle-bot-all.jar", "--beekeeperHost", "http://fge.martin.bkpr.link", "--beekeeperApiKey", "5f077247-c193-4beb-b989-29a858182084", "--googleServiceAccountJson", "/opt/bkpr/cred.json"]
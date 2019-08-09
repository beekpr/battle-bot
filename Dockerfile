FROM openjdk:8-jre

LABEL maintainer="Beekeeper AG <admin@beekeeper.io>"
LABEL Description="Battle Bot"

WORKDIR /opt/bkpr

ADD build/libs/battle-bot-all.jar /opt/bkpr
ADD BattleBot-ffe28c6779de.json /opt/bkpr/cred.json
ADD src/main/resources/gifs/* /opt/bkpr/src/main/resources/gifs/

ENTRYPOINT ["java"]
#CMD ["-jar", "/opt/bkpr/battle-bot-all.jar", "--beekeeperHost", "http://fge.martin.bkpr.link", "--beekeeperApiKey", "5f077247-c193-4beb-b989-29a858182084", "--googleServiceAccountJson", "/opt/bkpr/cred.json"]
CMD ["-jar", "/opt/bkpr/battle-bot-all.jar", "--beekeeperHost", "https://team.beekeeper.io", "--beekeeperApiKey", "d96df92b-f5aa-49aa-bb06-0275c7198175", "--googleServiceAccountJson", "/opt/bkpr/cred.json"]

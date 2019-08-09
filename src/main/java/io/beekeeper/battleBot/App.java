package io.beekeeper.battleBot;

import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import io.beekeeper.core.ApiClient;
import io.beekeeper.core.BeekeeperApi;
import io.beekeeper.battleBot.google.GoogleApiFactory;
import io.beekeeper.battleBot.survey.JWTSecrets;
import io.beekeeper.sdk.BeekeeperSDK;
import io.beekeeper.sdk.ChatBot;

public class App {

    public static void main(String[] args) {

        Options options = new Options();
        JCommander commander = new JCommander(options);
        commander.parse(args);

        JWTSecrets.setSecret(options.jwtSecret);
        GoogleApiFactory googleApiFactory = new GoogleApiFactory("Battle-Bot", options.googleServiceAccountJson);
        BeekeeperApi api = new BeekeeperApi(new ApiClient(options.beekeeperHost, options.beekeeperApiKey));
        BeekeeperSDK sdk = BeekeeperSDK.newInstance(options.beekeeperHost, options.beekeeperApiKey);

        ChatBot bot = BattleBot.create(api, sdk, googleApiFactory);
        bot.start();

    }

    private static class Options {
        @Parameter(description = "The url for the Beekeeper api. E.g. https://team.beekeeper.io for team",
                required = true,
                names = { "--beekeeperHost" })
        String beekeeperHost;

        @Parameter(description = "The Api access key for the account",
                required = true,
                names = { "--beekeeperApiKey" })
        String beekeeperApiKey;

        @Parameter(description = "The calendar id to check",
                required = true,
                names = { "--googleCalendarId" })
        String googleCalendarId;

        @Parameter(description = "The Api access file for Google API ",
                required = true,
                names = { "--googleServiceAccountJson" })
        String googleServiceAccountJson;

        @Parameter(
                description = "Secret for the JWT validation. Can be anything, but should remain constant between restarts",
                required = true,
                names = { "--jwtSecret" })
        String jwtSecret;
    }
}

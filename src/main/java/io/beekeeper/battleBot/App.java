package io.beekeeper.battleBot;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.beekeeper.battleBot.google.GoogleApiFactory;
import io.beekeeper.core.ApiClient;
import io.beekeeper.core.BeekeeperApi;
import io.beekeeper.sdk.BeekeeperSDK;

public class App {
    public static final String SHEET_ID = "1FC-sBLCPtTvvb74z6KpTOTZuQMtxYGKTF1fxs-HLTzU";
    public static void main(String[] args) {

        Options options = new Options();
        JCommander commander = new JCommander(options);
        commander.parse(args);

        GoogleApiFactory googleApiFactory = new GoogleApiFactory("Battle-Bot", options.googleServiceAccountJson);
        BeekeeperApi api = new BeekeeperApi(new ApiClient(options.beekeeperHost, options.beekeeperApiKey));
        BeekeeperSDK sdk = BeekeeperSDK.newInstance(options.beekeeperHost, options.beekeeperApiKey);

        BattleBot bot = new BattleBot(api, sdk, googleApiFactory, SHEET_ID);
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

        @Parameter(description = "The Api access file for Google API ",
                required = true,
                names = { "--googleServiceAccountJson" })
        String googleServiceAccountJson;
    }
}

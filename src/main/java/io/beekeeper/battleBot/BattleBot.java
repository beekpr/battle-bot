package io.beekeeper.battleBot;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.beekeeper.battleBot.google.GoogleApiFactory;
import io.beekeeper.battleBot.sheets.v1.DeveloperMetadataHelper;
import io.beekeeper.core.BeekeeperApi;
import io.beekeeper.sdk.BeekeeperSDK;
import io.beekeeper.sdk.ChatBot;
import io.beekeeper.sdk.exception.BeekeeperException;
import io.beekeeper.sdk.model.ConversationMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BattleBot extends ChatBot {

    private final Sheets sheetsService;
    private final DeveloperMetadataHelper metaData;
    private final BeekeeperApi api;
    private final BeekeeperSDK sdk;
    private final String battleSheetId;
    private final String COMPETITOR_SHEET_RANGE = "Competitors!A2:E";
    private Collection<Competitor> competitorData = new ArrayList<>();

    public BattleBot(
            BeekeeperApi api,
            BeekeeperSDK sdk,
            GoogleApiFactory googleApiFactory,
            String sheetId
    ) {
        super(sdk);
        this.api = api;
        this.sdk = sdk;
        this.sheetsService = googleApiFactory.getSheetsService();
        this.metaData = new DeveloperMetadataHelper(sheetsService);
        this.battleSheetId = sheetId;
        loadSheet();
    }

    private void loadSheet() {
        System.out.println("Loading Sheet with ID:" + battleSheetId);
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(battleSheetId, COMPETITOR_SHEET_RANGE)
                    .execute();
            this.competitorData = createCompetitorData(response);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            throw new RuntimeException("Failed to load competitor data");
        }
    }

    private Collection<Competitor> createCompetitorData(ValueRange response) {
        Map<String, Competitor> competitorByName = new HashMap<>();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            System.out.println("Battlebot Ready");
            for (List<Object> row : values) {
                try {
                    Competitor competitor = competitorByName.getOrDefault(
                            tryGetCell(row, 0),
                            Competitor
                                    .builder()
                                    .name(tryGetCell(row, 0))
                                    .commands(new ArrayList<>())
                                    .urls(new ArrayList<>())
                                    .description(tryGetCell(row, 2))
                                    .winRate(tryGetCell(row, 3))
                                    .build()
                    );
                    competitor.getCommands().add(tryGetCell(row, 1));
                    competitor.getUrls().add(tryGetCell(row, 4));
                    competitorByName.putIfAbsent(competitor.getName(), competitor);
                    printRow(row);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return competitorByName.values();
        }
        return Collections.emptyList();
    }

    private String tryGetCell(List<Object> row, int idx) {
        try{
            return row.get(idx).toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private void printRow(List<Object> row) {
        StringBuilder rowString = new StringBuilder();
        row.forEach(o -> rowString.append(o).append(", "));
        System.out.println(rowString);
    }

    @Override
    public void onNewMessage(ConversationMessage message, ConversationHelper conversationHelper) {
        try {
            processMessage(message);
        } catch (BeekeeperException e) {
            System.out.println(e.getMessage());
        }
    }

    private void processMessage(ConversationMessage message) throws BeekeeperException {
        int conversationId = message.getConversationId();
        String input = message.getText();
        String replyMessage;

        // Ignore if not a command
        if (!input.startsWith("/")) {
            return;
        }

        // TODO: Define more commands
        if (input.startsWith("/competitor")) {
            replyMessage = this.getCompetitorInformation(input.substring("/competitor".length() - 1));
        } else {
            // TODO: List available commands
            replyMessage = "Unknown command";
        }

        sendChatMessage(conversationId, replyMessage);
    }

    /**
     * Sends a message in the chat.
     *
     * @param conversationId - The ID of the current conversation.
     * @param message        - The message text to be sent in the chat.
     * @throws BeekeeperException - sendMessage can throw an Exception
     */
    private void sendChatMessage(int conversationId, String message) throws BeekeeperException {
        sdk.getConversations().sendMessage(conversationId, message).execute();
    }

    private String getCompetitorInformation(String competitorName) {
        return "There is no info on: " + competitorName;
    }
}

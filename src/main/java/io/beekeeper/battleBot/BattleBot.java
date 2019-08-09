package io.beekeeper.battleBot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import io.beekeeper.battleBot.google.GoogleApiFactory;
import io.beekeeper.sdk.BeekeeperSDK;
import io.beekeeper.sdk.ChatBot;
import io.beekeeper.sdk.core.BeekeeperApi;
import io.beekeeper.sdk.exception.BeekeeperException;
import io.beekeeper.sdk.exception.MediaTypeException;
import io.beekeeper.sdk.model.ConversationMessage;
import io.beekeeper.sdk.model.FileAttachment;
import io.beekeeper.sdk.params.SendMessageParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.beekeeper.battleBot.BotResources.COMPETITOR_NOT_FOUND;
import static io.beekeeper.battleBot.BotResources.COMPTETITOR_FOUND_INTRO;

public class BattleBot extends ChatBot {

    private final Sheets sheetsService;
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
            competitorData.forEach(c -> {
                try {
                    System.out.println(new ObjectMapper().writeValueAsString(c));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
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
            sendChatMessage(conversationId, BotResources.INTRO);
            sendBattleGif(conversationId);
            return;
        }

        if (input.equals("/list")) {
            replyMessage = competitorData
                    .stream()
                    .map(Competitor::getName)
                    .collect(Collectors.toList())
                    .toString()
                    .replace(",", "\n");
        } else if (input.startsWith("/")) {
            String requestedCompetitor = input.substring(1).toLowerCase();
            Optional<Competitor> competitor = competitorData
                    .stream()
                    .filter(c -> c.getName().toLowerCase().equals(requestedCompetitor))
                    .findFirst();
            if (competitor.isPresent()) {
                Competitor comp = competitor.get();
                replyMessage =
                        COMPTETITOR_FOUND_INTRO + "\n" +
                        "Name: " + comp.getName() + "\n" +
                        "Description: " + comp.getDescription() + "\n" +
                        "Win Rate: " + comp.getWinRate() + "\n" +
                        "URLs: " + comp.getUrls().toString().replace(",", "\n");
            } else {
                replyMessage = COMPETITOR_NOT_FOUND;
            }
        } else {
            // TODO: List commands
            replyMessage = "Unknown command";
        }

        sendChatMessage(conversationId, replyMessage);
    }

    private void sendBattleGif(int conversationId) {
        try {
            File file = new File(String.format("%s/src/main/resources/gifs/bring_it.gif", System.getProperty("user.dir")));
            InputStream targetStream = Files.asByteSource(file).openStream();
            FileAttachment battleGif = getSdk()
                    .getFiles()
                    .uploadPhoto(targetStream, file.length(), "image/gif")
                    .execute();
            sdk.getConversations().sendMessage(conversationId, SendMessageParams.builder().photoId(battleGif.getId()).build());
        } catch (Exception e) {
            e.printStackTrace();
        }

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

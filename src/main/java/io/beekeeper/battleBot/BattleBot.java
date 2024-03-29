package io.beekeeper.battleBot;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.io.Files;
import io.beekeeper.battleBot.google.GoogleApiFactory;
import io.beekeeper.sdk.BeekeeperSDK;
import io.beekeeper.sdk.ChatBot;
import io.beekeeper.sdk.core.BeekeeperApi;
import io.beekeeper.sdk.exception.BeekeeperException;
import io.beekeeper.sdk.model.ConversationMessage;
import io.beekeeper.sdk.model.FileAttachment;
import io.beekeeper.sdk.params.SendMessageParams;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static io.beekeeper.battleBot.BotResources.ANOTHER_COMMAND_PROMPT;
import static io.beekeeper.battleBot.BotResources.COMPETITOR_FOUND_INTROS;
import static io.beekeeper.battleBot.BotResources.COMPETITOR_NOT_FOUND;
import static io.beekeeper.battleBot.BotResources.NO_FOLLOW_UPS;
import static io.beekeeper.battleBot.BotResources.PROMPT_FOR_ANOTHER_COMMAND;
import static io.beekeeper.battleBot.BotResources.THIS_WAS_NOT_USEFUL;
import static io.beekeeper.battleBot.BotResources.THIS_WAS_USEFUL_ANSWER;
import static io.beekeeper.battleBot.BotResources.WAS_THIS_USEFUL;

public class BattleBot extends ChatBot {

    private final Sheets sheetsService;
    private final BeekeeperApi api;
    private final BeekeeperSDK sdk;
    private final String battleSheetId;
    private final String COMPETITOR_SHEET_RANGE = "Competitors!A2:E";
    private Collection<Competitor> competitorData = new ArrayList<>();

    private boolean lastMessageWasAnotherCommandPrompt = false;
    private boolean lastMessageWasQuestionIfBotIsUseful = false;

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
                                    .urls(new HashSet<>())
                                    .description(tryGetCell(row, 2))
                                    .winRate(tryGetCell(row, 3))
                                    .build()
                    );
                    if (competitor.getCommands() != null) {
                        competitor.getCommands().add(tryGetCell(row, 1));
                    }
                    if (competitor.getUrls() != null) {
                        competitor.getUrls().add(tryGetCell(row, 4));
                    }
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
        try {
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

        // TODO: Fix this so it is not ugly
        if (input.toLowerCase().equals("yes")) {
            if (lastMessageWasAnotherCommandPrompt) {
                // User answered YES to "Anything else I can help you with?"
                sendChatMessage(conversationId, ANOTHER_COMMAND_PROMPT);
                lastMessageWasAnotherCommandPrompt = true;
                return;
            } else if (lastMessageWasQuestionIfBotIsUseful) {
                // User answered YES to "Was this helpful to you?"
                sendChatMessage(conversationId, THIS_WAS_USEFUL_ANSWER);
                return;
            }
        } else if (input.toLowerCase().equals("no")) {
            if (lastMessageWasAnotherCommandPrompt) {
                // User answered NO to "Anything else I can help you with?"
                // Ask the user if he was happy with the bot
                sendChatMessage(conversationId, WAS_THIS_USEFUL);
                lastMessageWasQuestionIfBotIsUseful = true;
                lastMessageWasAnotherCommandPrompt = false;
                return;
            } else if (lastMessageWasQuestionIfBotIsUseful) {
                // User answered NO to "Was this useful?"
                sendChatMessage(conversationId, THIS_WAS_NOT_USEFUL);
                lastMessageWasAnotherCommandPrompt = false;
                return;
            }
        }

        // Ignore if not a command and not an answer to a previous question
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
                        getRandomString(COMPETITOR_FOUND_INTROS) + "\n" +
                                "\nName: " + comp.getName() + "\n" +
                                "\nDescription: " + comp.getDescription() + "\n" +
                                "Win Rate: " + comp.getWinRate() + "\n" +
                                "URLs: " + comp.getUrls().toString().replace(",", "\n");
            } else {
                replyMessage = COMPETITOR_NOT_FOUND;
            }
        } else {
            replyMessage = "Unknown command";
        }

        // Reply to the user's input
        sendChatMessage(conversationId, replyMessage);

        // Send follow up message
        sendChatMessage(conversationId, PROMPT_FOR_ANOTHER_COMMAND);
        lastMessageWasAnotherCommandPrompt = true;
    }

    private String getRandomString(List<String> input) {
        return input.get(new Random().nextInt(input.size()));
    }

    private void sendBattleGif(int conversationId) {
        try {
            String randomBattleUrl = getRandomBattleUrl();
            File file = new File(randomBattleUrl);
            InputStream targetStream = Files.asByteSource(file).openStream();
            FileAttachment battleGif = getSdk()
                    .getFiles()
                    .uploadPhoto(targetStream, file.length(), "image/gif")
                    .execute();
            sdk.getConversations().sendMessage(conversationId, SendMessageParams.builder().photoId(battleGif.getId()).build()).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String getRandomBattleUrl() {
        File f = new File(String.format("%s/src/main/resources/gifs/", System.getProperty("user.dir")));
        List<String> gifs = Arrays.asList(Objects.requireNonNull(f.list()));
        String gif = gifs.get(new Random().nextInt(gifs.size() - 1));
        return String.format("%s/src/main/resources/gifs/%s", System.getProperty("user.dir"), gif);
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

}

package io.beekeeper.battleBot;

import static io.beekeeper.battleBot.sheets.v1.SheetUtils.cell;
import static io.beekeeper.battleBot.sheets.v1.SheetUtils.row;
import static io.beekeeper.battleBot.sheets.v1.SheetUtils.rows;

import java.io.IOException;
import java.rmi.server.ExportException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;

import com.google.api.services.sheets.v4.model.ValueRange;
import io.beekeeper.core.BeekeeperApi;
import io.beekeeper.battleBot.google.GoogleApiFactory;
import io.beekeeper.battleBot.sheets.v1.AlreadyHandledException;
import io.beekeeper.battleBot.sheets.v1.DeveloperMetadataHelper;
import io.beekeeper.battleBot.sheets.v1.MetaDataKeys;
import io.beekeeper.battleBot.sheets.v1.SurveySpreadsheets;
import io.beekeeper.battleBot.survey.Survey;
import io.beekeeper.battleBot.survey.SurveyResponseJWT;
import io.beekeeper.battleBot.survey.SurveySender;
import io.beekeeper.battleBot.utils.Numbers;
import io.beekeeper.sdk.BeekeeperSDK;
import io.beekeeper.sdk.ChatBot;
import io.beekeeper.sdk.exception.BeekeeperException;
import io.beekeeper.sdk.model.ConversationMessage;

public class BattleBot extends ChatBot {

    private final Sheets sheetsService;
    private final DeveloperMetadataHelper metaData;
    private final BeekeeperApi api;
    private final BeekeeperSDK sdk;
    private final String battleSheetId;
    private final String SHEET_RANGE = "A2:E";

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
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(battleSheetId, SHEET_RANGE)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                System.out.println("No data found.");
            } else {
                System.out.println("Battlebot Ready");
                values.forEach(
                        row -> System.out.printf(
                                "%s, %s, %s, %s, %s\n",
                                row.get(0),
                                row.get(1),
                                row.get(2),
                                row.get(3),
                                row.get(4)
                        )
                );
            }
        } catch (Exception ex) {
            System.out.println(ex.getStackTrace());
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
     * @param message - The message text to be sent in the chat.
     * @throws BeekeeperException - sendMessage can throw an Exception
     */
    private void sendChatMessage(int conversationId, String message) throws BeekeeperException {
        sdk.getConversations().sendMessage(conversationId, message).execute();
    }

    private String getCompetitorInformation(String competitorName) {
        return "There is no info on: " + competitorName;
    }

    private static class EventHandler implements Consumer<Event> {

        private final ExecutorService pool = Executors.newFixedThreadPool(6);
        private final BeekeeperApi api;
        private final SurveySpreadsheets survey;

        public EventHandler(BeekeeperApi api, GoogleApiFactory googleApiFactory) {
            super();
            this.api = api;
            this.survey = new SurveySpreadsheets(
                    googleApiFactory.getDriveService(),
                    googleApiFactory.getSheetsService(),
                    new DeveloperMetadataHelper(googleApiFactory.getSheetsService())
            );
        }

        @Override
        public void accept(Event event) {
            try {
                System.out.println("Processing new event " + event.getSummary());

                Spreadsheet spreadsheet = survey.getSpreadSheetFor(event);
                Integer sheet = survey.getSheetFor(spreadsheet, event);
                List<Survey> surveys = survey.createSurveys(spreadsheet, sheet, event);

                event.getRecurringEventId();
                surveys
                        .stream()
                        .map(survey -> new SurveySender(api, survey))
                        .forEach(pool::submit);

            } catch (IOException e) {
                System.err.println("Failed to start the feedback gathering process for event " + event.getId());
                e.printStackTrace();
            } catch (AlreadyHandledException e) {
                System.err.println("This event was already handled " + event.getId());
            }
        }
    }
}

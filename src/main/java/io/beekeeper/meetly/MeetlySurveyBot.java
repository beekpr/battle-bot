package io.beekeeper.meetly;

import static io.beekeeper.meetly.sheets.v1.SheetUtils.cell;
import static io.beekeeper.meetly.sheets.v1.SheetUtils.row;
import static io.beekeeper.meetly.sheets.v1.SheetUtils.rows;

import java.io.IOException;
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

import io.beekeeper.core.BeekeeperApi;
import io.beekeeper.meetly.google.GoogleApiFactory;
import io.beekeeper.meetly.google.calendar.CalendarState;
import io.beekeeper.meetly.sheets.v1.AlreadyHandledException;
import io.beekeeper.meetly.sheets.v1.DeveloperMetadataHelper;
import io.beekeeper.meetly.sheets.v1.MetaDataKeys;
import io.beekeeper.meetly.sheets.v1.SurveySpreadsheets;
import io.beekeeper.meetly.survey.Survey;
import io.beekeeper.meetly.survey.SurveyResponseJWT;
import io.beekeeper.meetly.survey.SurveySender;
import io.beekeeper.meetly.utils.Numbers;
import io.beekeeper.sdk.BeekeeperSDK;
import io.beekeeper.sdk.ChatBot;
import io.beekeeper.sdk.model.ConversationMessage;

public class MeetlySurveyBot {

    public static ChatBot create(BeekeeperApi api,
                                 BeekeeperSDK sdk,
                                 GoogleApiFactory googleApiFactory,
                                 CalendarState state) {

        state.onMeetingEnd.listen(new EventHandler(api, googleApiFactory));
        ChatBot bot = new SurveyBot(sdk, googleApiFactory);
        return bot;
    }

    private static class SurveyBot extends ChatBot {

        private final Sheets sheetsService;
        private final DeveloperMetadataHelper metaData;

        public SurveyBot(BeekeeperSDK sdk, GoogleApiFactory googleApiFactory) {
            super(sdk);
            this.sheetsService = googleApiFactory.getSheetsService();
            this.metaData = new DeveloperMetadataHelper(sheetsService);
        }

        @Override
        public void onNewMessage(ConversationMessage message, ConversationHelper conversationHelper) {
            SurveyResponseJWT jwt = SurveyResponseJWT.decode(message.getText());
            if (jwt == null) {
                return;
            }

            Optional<Map<String, String>> map;
            try {
                map = metaData.getMap(jwt.getSpreadsheetId(), MetaDataKeys.responseIdData(jwt.getResponseId()));
                if (!map.isPresent()) {
                    System.err.println("Could not find metadata associated with response id " + jwt.getResponseId());
                    return;
                }

                Integer sheetId = Numbers.tryParse(map.get().get("sheetId"));
                Integer rowId = Numbers.tryParse(map.get().get("rowId"));

                if (sheetId == null || rowId == null) {
                    System.err.println("Incorrect metadata information.");
                    return;
                }
                DateTimeFormatter timeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);

                this.sheetsService.spreadsheets()
                    .batchUpdate(
                        jwt.getSpreadsheetId(),
                        new BatchUpdateSpreadsheetRequest().setRequests(
                            Arrays.asList(
                                new Request().setUpdateCells(
                                    new UpdateCellsRequest()
                                        .setStart(
                                            new GridCoordinate().setSheetId(sheetId)
                                                .setRowIndex(rowId)
                                                .setColumnIndex(1)
                                        )
                                        .setRows(
                                            rows(
                                                row(
                                                    cell(Numbers.tryParse(jwt.getValue())),
                                                    cell(LocalDateTime.now(ZoneId.of("Z")).format(timeFormat))
                                                )
                                            )
                                        )
                                        .setFields("*")
                                )
                            )
                        )
                    )
                    .execute();

            } catch (IOException e) {
                System.err.println("Could not get metadata for response id " + jwt.getResponseId());
                e.printStackTrace();
            }

        }

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

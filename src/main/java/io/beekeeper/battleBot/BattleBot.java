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
import io.beekeeper.sdk.model.ConversationMessage;

public class BattleBot extends ChatBot {

    private final Sheets sheetsService;
    private final DeveloperMetadataHelper metaData;
    private final BeekeeperApi api;
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

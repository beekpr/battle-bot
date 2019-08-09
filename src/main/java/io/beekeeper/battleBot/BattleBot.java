package io.beekeeper.battleBot;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.beekeeper.battleBot.google.GoogleApiFactory;
import io.beekeeper.battleBot.sheets.v1.DeveloperMetadataHelper;
import io.beekeeper.battleBot.sheets.v1.MetaDataKeys;
import io.beekeeper.battleBot.survey.SurveyResponseJWT;
import io.beekeeper.battleBot.utils.Numbers;
import io.beekeeper.core.BeekeeperApi;
import io.beekeeper.sdk.BeekeeperSDK;
import io.beekeeper.sdk.ChatBot;
import io.beekeeper.sdk.exception.BeekeeperException;
import io.beekeeper.sdk.model.ConversationMessage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.beekeeper.battleBot.sheets.v1.SheetUtils.*;

public class BattleBot extends ChatBot {

    private final Sheets sheetsService;
    private final DeveloperMetadataHelper metaData;
    private final BeekeeperApi api;
    private final String battleSheetId;
    private final String SHEET_RANGE = "Class Data!A2:E";

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
        System.out.println("Loading Sheet with ID:" + battleSheetId);
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
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void onNewMessage(ConversationMessage message, ConversationHelper conversationHelper) {
        try {
            conversationHelper.reply("Battle ready");
        } catch (BeekeeperException e) {
            e.printStackTrace();
        }
    }
}

package io.beekeeper.battleBot.sheets.v1;

import static io.beekeeper.battleBot.sheets.v1.SheetUtils.cell;
import static io.beekeeper.battleBot.sheets.v1.SheetUtils.formula;
import static io.beekeeper.battleBot.sheets.v1.SheetUtils.grid;
import static io.beekeeper.battleBot.sheets.v1.SheetUtils.grids;
import static io.beekeeper.battleBot.sheets.v1.SheetUtils.range;
import static io.beekeeper.battleBot.sheets.v1.SheetUtils.row;
import static io.beekeeper.battleBot.sheets.v1.SheetUtils.rows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.AppendCellsRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;

import io.beekeeper.battleBot.sheets.v1.SheetUtils.Charts;
import io.beekeeper.battleBot.sheets.v1.SheetUtils.Format;
import io.beekeeper.battleBot.utils.DateFormats;
import io.beekeeper.battleBot.utils.Numbers;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SurveySpreadsheets {

    private final Drive driveService;
    private final Sheets sheetsService;
    private final DeveloperMetadataHelper metaData;

    public Spreadsheet getSpreadSheetFor(Event event) throws IOException {
        FileList files = driveService.files().list().setQ(
            String.format("appProperties has { key='eventId' and value='%s' }", getEventId(event))
        ).execute();

        if (files.getFiles().isEmpty()) {
            return createSheetFor(event);
        } else {
            File file = files.getFiles().get(0);
            return sheetsService.spreadsheets().get(file.getId()).execute();
        }
    }

    public Spreadsheet createSheetFor(Event event) throws IOException {
        String title = String.format("Meeting Feedback for '%s'", event.getSummary());
        Spreadsheet spreadsheet = newSpreadsheet(title);

        // Mark the file so we can find it for recurrent events
        Map<String, String> appProperties = new HashMap<>();
        appProperties.put("eventId", getEventId(event));
        driveService.files()
            .update(spreadsheet.getSpreadsheetId(), new File().setAppProperties(appProperties))
            .execute();

        // Allow the event organizer to see the file
        driveService.permissions().create(
            spreadsheet.getSpreadsheetId(),
            new Permission().setEmailAddress(event.getOrganizer().getEmail()).setRole("reader").setType("user")
        ).execute();

        return spreadsheet;
    }

    private Spreadsheet newSpreadsheet(String title) throws IOException {
        Spreadsheet spreadSheet = new Spreadsheet().setProperties(new SpreadsheetProperties().setTitle(title));
        List<Sheet> sheets = new ArrayList<Sheet>();
        Sheet firstSheet = new Sheet();
        sheets.add(firstSheet);
        spreadSheet.setSheets(sheets);

        firstSheet.setProperties(new SheetProperties().setSheetId(0).setTitle("Summary"));

        firstSheet.setData(
            grids(
                grid(0, 0).setRowData(
                    rows(
                        row(cell("Summary", Format.bold))
                    )
                ),
                grid(0, 2).setRowData(
                    rows(
                        row(
                            cell("Meeting Id", Format.bold),
                            cell("Date", Format.bold),
                            cell("Rating", Format.bold),
                            cell("Response Rate", Format.bold),
                            cell("Participant Count", Format.bold)
                        )
                    )
                )
            )
        );

        firstSheet.setCharts(
            Arrays.asList(
                Charts.line("Rating over time", "Date", range(1, 2, 2, 1000), range(2, 3, 2, 1000))
                    .setPosition(Charts.position(600, 20, 800, 500)),

                Charts.line("Respone rate over time", "Date", range(1, 2, 2, 1000), range(3, 4, 2, 1000))
                    .setPosition(Charts.position(600, 540, 800, 500)),

                Charts.line("participant count over time", "Date", range(1, 2, 2, 1000), range(4, 5, 2, 1000))
                    .setPosition(Charts.position(600, 1080, 800, 500))
            )
        );

        return sheetsService.spreadsheets()
            .create(spreadSheet)
            .execute();
    }

    public Integer getSheetFor(Spreadsheet spreadsheet, Event event) throws IOException, AlreadyHandledException {
        System.out.println("Finding sheet for event");
        String spreadsheetId = spreadsheet.getSpreadsheetId();

        String metadataKey = MetaDataKeys.sheetIdForEvent(event);
        Optional<Integer> existingSheetId = metaData.get(spreadsheetId, metadataKey).map(Numbers::tryParse);
        if (existingSheetId.isPresent()) {
            System.out.println("Sheet already exists. Will not proceed");
            throw new AlreadyHandledException();
        }
        System.out.println("Creating a new sheet");

        Integer sheetId = spreadsheet.getSheets().size() + 1;
        String sheetTitle = getSheetTitle(event);
        AddSheetRequest request = new AddSheetRequest()
            .setProperties(
                new SheetProperties().setTitle(sheetTitle).setSheetId(sheetId).setIndex(2)
            );
        UpdateCellsRequest update = new UpdateCellsRequest()
            .setStart(new GridCoordinate().setSheetId(sheetId).setColumnIndex(0).setRowIndex(0))
            .setFields("*")
            .setRows(
                rows(
                    row(cell("Meeting Summary", Format.bold)),
                    row(cell("Id"), cell(event.getId())),
                    row(cell("Meeting Date"), cell(DateFormats.formatDate(event.getStart().getDateTime()))),
                    row(cell("Meeting Start"), cell(DateFormats.formatTime(event.getStart().getDateTime()))),
                    row(cell("Meeting End"), cell(DateFormats.formatTime(event.getEnd().getDateTime()))),
                    row(
                        cell("Participant Count"),
                        cell(
                            event.getAttendees()
                                .stream()
                                .filter(this::isParticipant)
                                .count()
                        )
                    ),
                    row(),
                    row(cell("Feedback Summary", Format.bold)),
                    row(cell("Feedback Responses"), formula("=COUNT(B15:B)")),
                    row(cell("Feedback Rating Average"), formula("=AVERAGE(B15:B)")),
                    row(cell("Feedback %"), formula("=(COUNT(B14:B) / COUNTA(A15:A)) * 100")),
                    row(),
                    row(cell("Feedback Details", Format.bold)),
                    row(cell("Attendee"), cell("Rating"), cell("Date Submitted"))
                )
            );

        AppendCellsRequest appendCells = new AppendCellsRequest().setSheetId(0)
            .setRows(
                rows(
                    row(
                        formula(String.format("='%s'!B2", sheetTitle)),
                        formula(String.format("='%s'!B3", sheetTitle)),
                        formula(String.format("='%s'!B10", sheetTitle)),
                        formula(String.format("='%s'!B11", sheetTitle)),
                        formula(String.format("='%s'!B6", sheetTitle))
                    )
                )
            )
            .setFields("*");

        UpdateDimensionPropertiesRequest columnUpdate = new UpdateDimensionPropertiesRequest()
            .setRange(
                new DimensionRange().setDimension("COLUMNS")
                    .setStartIndex(0)
                    .setEndIndex(3)
                    .setSheetId(sheetId)
            )
            .setProperties(new DimensionProperties().setPixelSize(250))
            .setFields("*");

        sheetsService.spreadsheets().batchUpdate(
            spreadsheetId,
            new BatchUpdateSpreadsheetRequest()
                .setRequests(
                    Arrays.asList(
                        new Request().setAddSheet(request),
                        new Request().setAppendCells(appendCells),
                        new Request().setUpdateCells(update),
                        new Request().setUpdateDimensionProperties(columnUpdate),
                        metaData.create(metadataKey, Integer.toString(sheetId))
                    )
                )
        ).execute();

        return sheetId;
    }



    private String getSheetTitle(Event event) {
        return String
            .format(
                "%s (%s)",
                event.getSummary(),
                DateFormats.formatDate(event.getStart().getDateTime())
            );
    }

    private boolean isParticipant(EventAttendee attendee) {
        if (!"accepted".equals(attendee.getResponseStatus())) {
            return false;
        }
        // Do not send the survey to the bot
        if (attendee.isSelf()) {
            return false;
        }
        // Do not send the survey to resource
        if (attendee.isResource()) {
            return false;
        }
        if (attendee.getEmail() == null || attendee.getEmail().contains(".calendar.google.com")) {
            return false;
        }
        return true;
    }

    private String getEventId(Event event) {
        String eventId = event.getRecurringEventId() != null ? event.getRecurringEventId() : event.getId();
        return eventId;
    }

}

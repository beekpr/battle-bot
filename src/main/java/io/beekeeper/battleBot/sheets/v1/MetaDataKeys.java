package io.beekeeper.battleBot.sheets.v1;

import com.google.api.services.calendar.model.Event;

public class MetaDataKeys {

    public static String sheetIdForEvent(Event event) {
        return "io.battleBot.spreadsheets.metadata.sheetid.event." + event.getId();
    }

    public static String responseIdData(String responseId) {
        return "io.battleBot.spreadsheets.metadata.responseId." + responseId;
    }

}

package io.beekeeper.battleBot.survey;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Survey {
    private final Event event;
    private final EventAttendee attendee;

    private final String spreadsheetId;
    private final Integer sheetId;
    private final String responseId;
}

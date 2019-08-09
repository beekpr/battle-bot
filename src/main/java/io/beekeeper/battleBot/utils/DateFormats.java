package io.beekeeper.battleBot.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.google.api.client.util.DateTime;

public class DateFormats {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String formatTime(DateTime time) {
        Instant instant = Instant.ofEpochMilli(time.getValue());
        return TIME_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    public static String formatDate(DateTime date) {
        Instant instant = Instant.ofEpochMilli(date.getValue());
        return DATE_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}

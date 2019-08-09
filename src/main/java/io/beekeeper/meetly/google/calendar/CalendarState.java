package io.beekeeper.meetly.google.calendar;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import io.beekeeper.meetly.google.GoogleApiFactory;
import io.beekeeper.sdk.util.EventSource;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CalendarState {

    public final EventSource<Event> onMeetingEnd = new EventSource<>();

    private final GoogleApiFactory factory;
    private final String calendarId;

    public void run() throws IOException {
        Thread thread = new Thread(new Updater(factory.getCalendarService(), calendarId, this));
        thread.start();
    }

    @RequiredArgsConstructor
    private class Updater implements Runnable {

        private final Calendar calendar;
        private final String calendarId;
        private final CalendarState state;

        @Override
        public void run() {
            long checkInterval = TimeUnit.MINUTES.toMillis(5);
            while (true) {
                DateTime minutesAgo = new DateTime(Date.from(Instant.now().minus(checkInterval, ChronoUnit.MILLIS)));
                DateTime minutesFronNow = new DateTime(Date.from(Instant.now().plus(checkInterval, ChronoUnit.MILLIS)));
                DateTime now = new DateTime(Date.from(Instant.now()));

                try {
                    Events events = calendar.events()
                        .list(calendarId) //
                        .setMaxResults(50)
                        .setTimeMin(minutesAgo)
                        .setTimeMax(now)
                        .setOrderBy("starttime")
                        .setSingleEvents(true)
                        .execute();

                    List<Event> items = events.getItems();
                    for (Event event : items) {
                        if (event.getStart().getDateTime() == null || event.getEnd().getDateTime() == null) {
                            // This is a full time event, ignore it
                            continue;
                        }
                        if (event.getEnd().getDateTime().getValue() > minutesFronNow.getValue()) {
                            // Do not deal with events ending too far in the
                            // future
                            continue;
                        }
                        state.onMeetingEnd.trigger(event);
                    }

                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    Thread.sleep(checkInterval);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

}

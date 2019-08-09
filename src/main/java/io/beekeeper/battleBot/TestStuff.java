package io.beekeeper.battleBot;

import java.io.IOException;
import java.util.Arrays;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Event.Organizer;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import io.beekeeper.battleBot.google.GoogleApiFactory;
import io.beekeeper.battleBot.google.calendar.CalendarState;

public class TestStuff {
    public static void test(GoogleApiFactory googleApiFactory, CalendarState state) {
        FileList list;
        try {
            list = googleApiFactory.getDriveService().files().list().execute();
            for (File f : list.getFiles()) {
                googleApiFactory.getDriveService().files().delete(f.getId()).execute();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        state.onMeetingEnd.trigger(
            new Event().setSummary("Blob")
                .setId("adsa")
                .setOrganizer(new Organizer().setEmail("filip@beekeeper.io"))
                .setStart(
                    new EventDateTime()
                        .setDate(new DateTime(System.currentTimeMillis()))
                        .setDateTime(new DateTime(System.currentTimeMillis()))
                )
                .setEnd(
                    new EventDateTime()
                        .setDate(new DateTime(System.currentTimeMillis() + 10000))
                        .setDateTime(new DateTime(System.currentTimeMillis() + 10000))
                )
                .setAttendees(
                    Arrays.asList(
                        new EventAttendee().setEmail("filip@beekeeper.io")
                            .setDisplayName("Filip Wieladek")
                            .setResponseStatus("accepted")
                    )
                )
        );
    }
}

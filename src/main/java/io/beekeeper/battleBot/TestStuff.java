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

public class TestStuff {
    public static void test(GoogleApiFactory googleApiFactory) {
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
    }
}

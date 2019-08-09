package io.beekeeper.battleBot.google;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GoogleApiFactory {

    private final String applicationName;
    private final String credentialsPath;

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    private Credential authorize(List<String> SCOPES) {
        try {
            return GoogleCredential.fromStream(new FileInputStream(credentialsPath)).createScoped(SCOPES);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build and return an authorized Sheets client service.
     *
     * @return an authorized Sheets client service
     * @throws IOException
     */
    public Sheets getSheetsService() {
        Credential credential = authorize(Arrays.asList(SheetsScopes.DRIVE_FILE));
        return new Sheets.Builder(Utils.getDefaultTransport(), Utils.getDefaultJsonFactory(), credential)
            .setApplicationName(applicationName).build();
    }

    /**
     * Build and return an authorized Drive client service.
     *
     * @return an authorized Sheets client service
     * @throws IOException
     */
    public Drive getDriveService() {
        Credential credential = authorize(Arrays.asList(DriveScopes.DRIVE_FILE));
        return new Drive.Builder(Utils.getDefaultTransport(), Utils.getDefaultJsonFactory(), credential)
            .setApplicationName(applicationName).build();
    }
}

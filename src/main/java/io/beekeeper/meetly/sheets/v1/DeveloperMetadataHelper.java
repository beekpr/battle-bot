package io.beekeeper.meetly.sheets.v1;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.CreateDeveloperMetadataRequest;
import com.google.api.services.sheets.v4.model.DataFilter;
import com.google.api.services.sheets.v4.model.DeveloperMetadata;
import com.google.api.services.sheets.v4.model.DeveloperMetadataLocation;
import com.google.api.services.sheets.v4.model.DeveloperMetadataLookup;
import com.google.api.services.sheets.v4.model.MatchedDeveloperMetadata;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SearchDeveloperMetadataRequest;
import com.google.api.services.sheets.v4.model.SearchDeveloperMetadataResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeveloperMetadataHelper {

    public final Sheets sheets;
    private final ObjectMapper mapper = new ObjectMapper();

    public Optional<String> get(String spreadsheetId, String key) throws IOException {
        SearchDeveloperMetadataResponse metadataResponse = sheets.spreadsheets()
            .developerMetadata()
            .search(
                spreadsheetId,
                new SearchDeveloperMetadataRequest().setDataFilters(
                    Arrays.asList(
                        new DataFilter()
                            .setDeveloperMetadataLookup(
                                new DeveloperMetadataLookup()
                                    .setMetadataLocation(new DeveloperMetadataLocation().setSpreadsheet(true))
                                    .setVisibility("PROJECT")
                                    .setMetadataKey(key)
                            )
                    )
                )
            )
            .execute();

        List<MatchedDeveloperMetadata> metadata = metadataResponse.getMatchedDeveloperMetadata();
        if (metadata != null) {
            return metadata
                .stream()
                .map(m -> m.getDeveloperMetadata())
                .map(m -> m.getMetadataValue())
                .findFirst();
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, String>> getMap(String spreadsheetId, String key) throws IOException {
        Optional<String> value = get(spreadsheetId, key);
        if (!value.isPresent()) {
            return Optional.empty();
        }

        String stringValue = value.get();
        return Optional.ofNullable(mapper.readValue(stringValue, Map.class));
    }

    public Request create(String key, String value) {
        return new Request().setCreateDeveloperMetadata(
            new CreateDeveloperMetadataRequest()
                .setDeveloperMetadata(
                    new DeveloperMetadata().setMetadataKey(key)
                        .setMetadataValue(value)
                        .setVisibility("PROJECT")
                        .setLocation(new DeveloperMetadataLocation().setSpreadsheet(true))
                )
        );
    }

    public Request create(String key, Map<String, String> map) throws IOException {
        try {
            String string = mapper.writeValueAsString(map);
            return create(key, string);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }
}

package io.beekeeper.meetly.survey;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SurveyResponseJWT {

    public static final String JWT_RESPONSE_VALUE = "v";
    public static final String JWT_RESPONSE_ID = "rsp";
    public static final String JWT_SPREADSHEET_ID = "ssi";

    private String value;
    private String responseId;
    private String spreadsheetId;

    public String encode() {
        return JWT.create()
            .withClaim(JWT_SPREADSHEET_ID, getSpreadsheetId())
            .withClaim(JWT_RESPONSE_ID, getResponseId())
            .withClaim(JWT_RESPONSE_VALUE, value)
            .sign(JWTSecrets.algorithmHS);
    }

    public static SurveyResponseJWT decode(String text) {
        DecodedJWT jwt = null;
        try {
            jwt = JWT.require(JWTSecrets.algorithmHS).build().verify(text);
        } catch (Exception e) {
            return null;
        }

        String spreadsheetId = jwt.getClaim(JWT_SPREADSHEET_ID).asString();
        String responseId = jwt.getClaim(JWT_RESPONSE_ID).asString();
        String value = jwt.getClaim(JWT_RESPONSE_VALUE).asString();

        if (spreadsheetId == null || responseId == null || value == null) {
            System.err.println("Incorrect JWT. Did not have the expected values.");
            return null;
        }

        return SurveyResponseJWT.builder()
            .responseId(responseId)
            .spreadsheetId(spreadsheetId)
            .value(value)
            .build();
    }

}

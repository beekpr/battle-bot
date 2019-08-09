package io.beekeeper.battleBot.survey;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;

import io.beekeeper.core.BeekeeperApi;
import io.beekeeper.core.model.ActionObject.ActionTypeEnum;
import io.beekeeper.core.model.AddonObject;
import io.beekeeper.core.model.AddonObject.AddonTypeEnum;
import io.beekeeper.core.model.ArchiveConversationActionObject;
import io.beekeeper.core.model.Conversation;
import io.beekeeper.core.model.InputOptionModel;
import io.beekeeper.core.model.InputOptionModel.OptionTypeEnum;
import io.beekeeper.core.model.InputOptionsAddonFieldsModel;
import io.beekeeper.core.model.InputOptionsAddonFieldsModel.InputTypeEnum;
import io.beekeeper.core.model.InputOptionsAddonObject;
import io.beekeeper.core.model.NewConversation;
import io.beekeeper.core.model.NewMessage;
import io.beekeeper.core.model.SendMessageActionFieldsModel;
import io.beekeeper.core.model.SendMessageActionFieldsModel.MessageTypeEnum;
import io.beekeeper.core.model.SendMessageActionObject;
import io.beekeeper.core.model.UserResource;
import io.beekeeper.battleBot.survey.SurveyResponseJWT.SurveyResponseJWTBuilder;
import io.beekeeper.battleBot.utils.SDKHelper;
import lombok.RequiredArgsConstructor;
import retrofit2.Response;

@RequiredArgsConstructor
public class SurveySender implements Runnable {

    private final BeekeeperApi api;
    private final Survey survey;

    @Override
    public void run() {
        Event event = survey.getEvent();
        EventAttendee attendee = survey.getAttendee();

        Instant eventStart = Instant.ofEpochMilli(event.getStart().getDateTime().getValue());

        System.out.println("Sending Survey for event " + event.getSummary() + " to attendee " + attendee.getEmail());

        DateTimeFormatter timeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        String surveyName = String.format(
            "Meetly: %s (%s)",
            event.getSummary(),
            LocalDateTime.ofInstant(eventStart, ZoneId.of("Z")).format(timeFormat)
        );

        try {
            // NOTE: This only works for hive as tenantuserid == hive email
            Response<UserResource> response = api.getUsersApi()
                .getUserByTenantUserId(attendee.getEmail())
                .execute();

            if (!response.isSuccessful()) {
                System.err.println("Failed to retrieve user " + attendee.getEmail());
                return;
            }

            UserResource surveyReceiver = response.body();
            Conversation conversation = SDKHelper.exec(
                api.getConversationsApi()
                    .postConversations(
                        new NewConversation()
                            .name(surveyName)
                            .group(true)
                            .usernames(Arrays.asList(surveyReceiver.getName()))
                    )
            );
            if (conversation == null) {
                System.err.println("Failed to create conversation user " + attendee.getEmail());
                return;
            }

            SDKHelper.exec(
                api.getConversationsApi().postConversationMessages(
                    conversation.getId(),
                    message(
                        "Hey! Congrats on wrapping up that meeting!\n\n"
                            + "We always strive to improve our meetings. "
                            + "It would be great if you quickly rate the meeting!\n\n"
                            + "Much obliged!"
                    ).addAddonsItem(inputOptions(survey))
                )
            );
        } catch (IOException e) {
            System.err.println("Failed sending survey to " + attendee.getEmail());
            e.printStackTrace();
        }

    }

    private NewMessage message(String string) {
        return new NewMessage().text(string);
    }

    private AddonObject inputOptions(Survey survey) {
        SurveyResponseJWTBuilder responseJWTBuilder = SurveyResponseJWT.builder()
            .spreadsheetId(survey.getSpreadsheetId())
            .responseId(survey.getResponseId());

        return new InputOptionsAddonObject()
            .inputOptions(
                new InputOptionsAddonFieldsModel()
                    .prompt("How would you rate the meeting? (1-Bad to 5-Great)")
                    .inputType(InputTypeEnum.SINGLE_SELECT)
                    .addOptionsItem(option("5", responseJWTBuilder.value("5").build().encode()))
                    .addOptionsItem(option("4", responseJWTBuilder.value("4").build().encode()))
                    .addOptionsItem(option("3", responseJWTBuilder.value("3").build().encode()))
                    .addOptionsItem(option("2", responseJWTBuilder.value("2").build().encode()))
                    .addOptionsItem(option("1", responseJWTBuilder.value("1").build().encode()))
            ).addonType(AddonTypeEnum.INPUT_OPTIONS);
    }

    private InputOptionModel option(String prompt, String response) {
        return new InputOptionModel()
            .prompt(prompt)
            .optionType(OptionTypeEnum.BUTTON)
            .addActionsItem(
                new SendMessageActionObject()
                    .sendMessage(
                        new SendMessageActionFieldsModel()
                            .messageType(MessageTypeEnum.REGULAR)
                            .text(response)
                    )
                    .actionType(ActionTypeEnum.SEND_MESSAGE)
            )
            .addActionsItem(
                new ArchiveConversationActionObject()
                    .actionType(ActionTypeEnum.ARCHIVE_CONVERSATION)
            );
    }
}
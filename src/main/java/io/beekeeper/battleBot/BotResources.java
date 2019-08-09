package io.beekeeper.battleBot;

import java.util.Arrays;
import java.util.List;

public class BotResources {
    public static final String INTRO = "\uD83D\uDC4B\uD83C\uDFFCHi there! What can I help you with today? Here's what I can do:\n" +
            "\n" +
            "* Type in /{{competitor's name}} to find more information about a specific competitor you are going up against\n" +
            "* If you would like to have a list of all our documented competitors, type in /list\n" +
            "* Have you come across a new competitor? Type in /add\n" +
            "* Use /{{contact's email adress}} to fetch everything related to the person you are meeting in your upcoming pitch";

    public static final String COMPETITOR_NOT_FOUND = "Oh I am sorry, it looks like I could not find what you are looking for \uD83D\uDE1FUse /list to get a list of all our documented competitors or use this Google form to add an insight about a new one: https://docs.google.com/forms/d/e/1FAIpQLSfMV6oPfRD2Zy24raRA-VlgRileQUcbIzW29OTSNlsF8sGLMA/viewform";

    public static final List<String> COMPETITOR_FOUND_INTROS = Arrays.asList(
            "\uD83D\uDE30Ouch! That hurts. I could find this:",
            "\uD83D\uDE29Uuugh - good one! Here is what I could find: ",
            "\uD83D\uDE33Pew, these guys stand no chance against us. Find out more here: \n"
    );

    public static final String PROMPT_FOR_ANOTHER_COMMAND = "Anything else I can help you with? Type in Yes or No.";
    public static final String ANOTHER_COMMAND_PROMPT = "Alright: what else can I do for you?\n" +
            "\n" +
            "* Type in /{{the competitor's name}} to find more information about a specific competitor you are going up against\n" +
            "* If you would like to have a list of all our documented competitors, type in /list\n" +
            "* Have you come across a new competitor? Type in /add\n" +
            "* Use /{{your contact's email adress}} to fetch everything related to the person you are meeting in your upcoming pitch\"";

    public static final List<String> NO_FOLLOW_UPS = Arrays.asList(
            "Well then... GOOD LUCK! \uD83D\uDC37\uD83C\uDF40",
            "I'll cross my fingers for you then! \uD83E\uDD1E\uD83C\uDFFD",
            "Good, then take a deep breath and let's rock this! \uD83D\uDCAA\uD83C\uDFFE"
    );

    public static final String WAS_THIS_USEFUL = "Was this useful to you? Type in Yes or No.";
    public static final String THIS_WAS_USEFUL_ANSWER = "Cool! ✌\uD83C\uDFFFI'm happy to hear that. Come back to me anytime if you need anything else.";
    public static final String THIS_WAS_NOT_USEFUL = "Yikes! \uD83D\uDE48I am always trying to improve my services, so please let me know here what else you would like me to be able to do: [URL TO A GOOGLE FORM FOR FEEDBACK OR PRODUCTBOARD?]";
}

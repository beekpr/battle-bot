package io.beekeeper.battleBot;

public class BotResources {
    public static final String INTRO = "\uD83D\uDC4B\uD83C\uDFFCHi there! What can I help you with today? Here's what I can do:\n" +
            "\n" +
            "* Type in /{{competitor's name}} to find more information about a specific competitor you are going up against\n" +
            "* If you would like to have a list of all our documented competitors, type in /list\n" +
            "* Have you come across a new competitor? Type in /add\n" +
            "* Use /{{contact's email adress}} to fetch everything related to the person you are meeting in your upcoming pitch";

    public static final String COMPETITOR_NOT_FOUND = "Oh I am sorry, it looks like I could not find what you are looking for \uD83D\uDE1FUse /list to get a list of all our documented competitors or use this Google form to add an insight about a new one: https://docs.google.com/forms/d/e/1FAIpQLSfMV6oPfRD2Zy24raRA-VlgRileQUcbIzW29OTSNlsF8sGLMA/viewform";

    public static final String COMPTETITOR_FOUND_INTRO = "\uD83D\uDE29Uuugh - good one! Here is what I could find: ";

}

package io.beekeeper.battleBot;

import com.google.common.io.Resources;

import java.io.File;
import java.net.URISyntaxException;

public class TestStuff {

    public static void main(String[] args) throws URISyntaxException {
       File f = new File(String.format("%s/src/main/resources/gifs/bring_it.gif", System.getProperty("user.dir")));
       System.out.println(f.exists());
    }
}

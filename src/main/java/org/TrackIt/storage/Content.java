package org.TrackIt.storage;

public class Content {
    public static String[] getButtonsContent(String type) {
        String[] buttonsContent = new String[0];

        switch(type) {
            case "main" -> buttonsContent = new String[]{ "START", "EXIT" };
        }

        return buttonsContent;
    }
}
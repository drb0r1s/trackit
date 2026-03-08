package org.TrackIt.panels;

import org.TrackIt.storage.Content;
import org.TrackIt.trackItSwing.Manager;
import org.TrackIt.trackItSwing.TrackItButton;
import org.TrackIt.trackItSwing.TrackItPanel;

public class ButtonPanel extends TrackItPanel {
    Manager manager = new Manager(this);
    private String[] buttonsContent;

    public ButtonPanel(String type) {
        manager.addBoxLayout("X");
        buttonsContent = Content.getButtonsContent(type);

        for(String buttonContent : buttonsContent) {
            TrackItButton button = new TrackItButton(buttonContent);
            button.onClick(e -> {
                switch(buttonContent.toLowerCase()) {
                    case "start" -> manager.switchPanels(this.getParent(), new VideosPanel());
                    case "exit" -> System.exit(0);
                }
            });

            add(button);
        }
    }
}

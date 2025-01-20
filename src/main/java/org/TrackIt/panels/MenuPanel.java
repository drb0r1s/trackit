package org.TrackIt.panels;

import org.TrackIt.trackItSwing.Manager;
import org.TrackIt.trackItSwing.TrackItLabel;
import org.TrackIt.trackItSwing.TrackItPanel;

public class MenuPanel extends TrackItPanel {
    Manager manager = new Manager(this);

    public MenuPanel() {
        TrackItLabel titleLabel = new TrackItLabel("TrackIt");
        ButtonPanel buttonPanel = new ButtonPanel("main");

        manager.spacer();
        add(titleLabel);

        manager.marginY(15);

        add(buttonPanel);

        manager.spacer();
    }
}

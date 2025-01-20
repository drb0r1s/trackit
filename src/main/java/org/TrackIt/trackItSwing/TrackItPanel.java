package org.TrackIt.trackItSwing;

import org.TrackIt.storage.Colors;

import javax.swing.*;

public class TrackItPanel extends JPanel {
    public Manager manager = new Manager(this);

    public TrackItPanel() {
        manager.addBoxLayout("Y");
        setBackground(Colors.DARK_GREY);
    }
}
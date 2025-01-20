package org.TrackIt.trackItSwing;

import org.TrackIt.storage.Colors;
import org.TrackIt.storage.Fonts;

import javax.swing.*;

public class TrackItLabel extends JLabel {
    public TrackItLabel() {
        setAlignmentX(JFrame.CENTER_ALIGNMENT);
    }

    public TrackItLabel(String text) {
        setText(text);
        setFont(Fonts.TITLE);
        setForeground(Colors.LIGHT_GREY);
        setAlignmentX(JFrame.CENTER_ALIGNMENT);
    }
}
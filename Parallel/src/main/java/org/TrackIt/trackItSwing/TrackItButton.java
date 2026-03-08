package org.TrackIt.trackItSwing;

import org.TrackIt.storage.Colors;
import org.TrackIt.storage.Fonts;
import org.TrackIt.style.Hover;

import javax.swing.*;
import java.awt.*;

public class TrackItButton extends JButton {
    public TrackItButton(String text) {
        setText(text);
        setFont(Fonts.BUTTON);

        setForeground(Colors.LIGHT_GREY);
        setBackground(Colors.CYAN);

        setBorderPainted(false);
        setFocusable(false);
        setFocusPainted(false);

        setAlignmentX(Component.CENTER_ALIGNMENT);
        addMouseListener(new Hover());
    }

    public void onClick(java.awt.event.ActionListener action) {
        this.addActionListener(action);
    }
}

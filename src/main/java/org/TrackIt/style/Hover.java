package org.TrackIt.style;

import org.TrackIt.storage.Colors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Hover extends MouseAdapter {
    @Override
    public void mouseEntered(MouseEvent e) {
        JButton button = (JButton) e.getSource();

        button.setBackground(Colors.BLUE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        JButton button = (JButton) e.getSource();
        button.setBackground(Colors.CYAN);
    }
}

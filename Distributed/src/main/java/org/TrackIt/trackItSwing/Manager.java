package org.TrackIt.trackItSwing;

import javax.swing.*;
import java.awt.*;

public class Manager {
    Container element;

    public Manager(Container element) {
        this.element = element;
    }

    public void refresh(Container element) {
        element.revalidate();
        element.repaint();
    }

    public void switchPanels(Container panel, Container newPanel) {
        Container parent = panel.getParent();

        parent.remove(panel);
        parent.add(newPanel);

        refresh(parent);
    }

    public void addBoxLayout(String type) {
        if(type.equals("X")) element.setLayout(new BoxLayout(element, BoxLayout.X_AXIS));
        else element.setLayout(new BoxLayout(element, BoxLayout.Y_AXIS));
    }

    public void spacer() {
        element.add(Box.createVerticalGlue());
    }

    public void marginX(int width) {
        element.add(Box.createHorizontalStrut(width));
    }

    public void marginY(int height) {
        element.add(Box.createVerticalStrut(height));
    }
}
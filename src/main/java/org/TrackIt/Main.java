package org.TrackIt;

import org.TrackIt.panels.MenuPanel;
import org.TrackIt.trackItSwing.Manager;

import javax.swing.*;

public class Main extends JFrame {
    public Main() {
        Manager manager = new Manager(this);

        initialization();

        MenuPanel menuPanel = new MenuPanel();
        add(menuPanel);

        manager.refresh(this);
    }

    private void initialization() {
        setTitle("TrackIt");
        setSize(800, 600);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setVisible(true);
    }

    public static void main(String[] args) {
        new Main();
    }
}
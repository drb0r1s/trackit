package org.TrackIt.panels;

import org.TrackIt.trackItSwing.Manager;
import org.TrackIt.trackItSwing.TrackItButton;
import org.TrackIt.trackItSwing.TrackItLabel;
import org.TrackIt.trackItSwing.TrackItPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class VideosPanel extends TrackItPanel {
    Manager manager = new Manager(this);

    public VideosPanel() {
        TrackItLabel titleLabel = new TrackItLabel("Choose the video");

        TrackItPanel buttonPanel = new TrackItPanel();
        buttonPanel.setLayout(new GridLayout(1, 4, 2, 2));

        File directory = new File("videos");
        File[] files = directory.listFiles();

        TrackItButton closeButton = new TrackItButton("CLOSE");

        closeButton.onClick(e -> {
            manager.switchPanels(this, new MenuPanel());
        });

        manager.spacer();

        add(titleLabel);
        manager.marginY(20);

        add(buttonPanel);

        for(int i = 0; i < files.length; i++) {
            TrackItPanel videoHolderPanel = new TrackItPanel();
            videoHolderPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

            ImageIcon thumbnail = new ImageIcon("./thumbnails/highway" + i + ".jpg");

            Image thumbnailImage = thumbnail.getImage();
            Image scaledThumbnailImage = thumbnailImage.getScaledInstance(200, 112, Image.SCALE_SMOOTH); // Resize

            ImageIcon scaledThumbnail = new ImageIcon(scaledThumbnailImage);
            JLabel thumbnailPanel = new JLabel(scaledThumbnail);

            String[] fileNameSplitted = files[i].getName().split("\\.");
            String fileName = fileNameSplitted[0];

            TrackItButton fileButton = new TrackItButton(fileName);

            fileButton.onClick(e -> {
                manager.switchPanels(this, new VideoPanel(fileName));
            });

            buttonPanel.add(videoHolderPanel);

            videoHolderPanel.add(thumbnailPanel);
            videoHolderPanel.manager.marginY(10);
            videoHolderPanel.add(fileButton);

            manager.marginY(10);
        }

        manager.marginY(10);
        add(closeButton);

        manager.spacer();
    }
}

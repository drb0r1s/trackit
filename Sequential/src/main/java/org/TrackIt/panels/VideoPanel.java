package org.TrackIt.panels;

import org.TrackIt.storage.Fonts;
import org.TrackIt.trackItSwing.*;
import org.TrackIt.videoPlayer.VideoPlayer;

public class VideoPanel extends TrackItPanel {
    private final Manager manager = new Manager(this);
    private final VideoPlayer videoPlayer = new VideoPlayer();

    public VideoPanel(String name) {
        TrackItLabel titleLabel = new TrackItLabel(name);
        TrackItVideo video = new TrackItVideo(640, 360, videoPlayer);

        TrackItLabel groupLabel = new TrackItLabel("Group objects:");
        groupLabel.setFont(Fonts.TEXT);

        TrackItButton groupButton = new TrackItButton("DISABLED");

        groupButton.onClick(e -> {
            groupButton.setText(videoPlayer.groupObjects ? "DISABLED" : "ENABLED");
            videoPlayer.groupObjects = !videoPlayer.groupObjects;
        });

        TrackItButton closeButton = new TrackItButton("CLOSE");

        closeButton.onClick(e -> {
            manager.switchPanels(this, new VideosPanel());
        });

        manager.spacer();
        add(titleLabel);

        manager.marginY(15);

        add(video);
        video.play("./videos/" + name + ".mp4");

        manager.marginY(15);

        add(videoPlayer.replayButton);

        manager.marginY(10);

        add(groupLabel);
        manager.marginY(10);
        add(groupButton);

        manager.marginY(20);
        add(closeButton);

        manager.spacer();
    }
}
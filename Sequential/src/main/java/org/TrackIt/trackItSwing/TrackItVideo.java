package org.TrackIt.trackItSwing;

import org.TrackIt.videoPlayer.VideoPlayer;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TrackItVideo extends TrackItLabel {
    private final int width;
    private final int height;
    private final VideoPlayer videoPlayer;

    public TrackItVideo(int width, int height, VideoPlayer videoPlayer) {
        this.width = width;
        this.height = height;
        this.videoPlayer = videoPlayer;

        setPreferredSize(new Dimension(width, height));
    }

    public void play(String path) {
        try {
            long duration = getDuration(path);

            String scale = "scale=" + width + ":" + height;

            ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg",
                "-i", path,
                "-vf", scale,
                "-r", "60",
                "-q:v", "2",
                "-f", "image2pipe",
                "-vcodec", "mjpeg",
                "-"
            );

            videoPlayer.display(builder, this, duration);
        }

        catch(Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static long getDuration(String path) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffprobe", "-i", path,
                    "-show_entries", "format=duration",
                    "-v", "quiet", "-of", "csv=p=0"
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;

                if((line = reader.readLine()) != null) {
                    double durationInSeconds = Double.parseDouble(line.trim());
                    return Math.round(durationInSeconds * 1000);
                }
            }

            process.waitFor();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return -1;
    }
}

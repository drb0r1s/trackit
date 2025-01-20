package org.TrackIt.videoPlayer;

import org.TrackIt.trackItSwing.TrackItButton;
import org.TrackIt.trackItSwing.TrackItVideo;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.*;

public class VideoPlayer {
    private ProcessBuilder builder;
    private TrackItVideo video;
    private long duration;

    public boolean replay = false;
    public boolean groupObjects = false;

    private Timer durationTimer = null;
    private Timer frameTimer = null;

    private BufferedImage prevFrame = null;
    private BufferedImage prevMask = null;

    private int maxKey = 1;

    public TrackItButton replayButton;

    public VideoPlayer() {
        replayButton = new TrackItButton("DETECTING MOVEMENT...");

        replayButton.onClick(e -> {
            if(replay) {
                try {
                    replay = false;
                    replayButton.setText("DETECTING MOVEMENT...");

                    display(builder, video, duration);
                } catch (Exception ex) {
                    System.out.println("Error: " + ex.getMessage());
                }
            }
        });
    }

    public void display(ProcessBuilder builder, TrackItVideo video, long duration) throws IOException {
        this.builder = builder;
        this.video = video;
        this.duration = duration;

        Process process = builder.start();

        durationTimer = new Timer((int) (duration - duration / 3), e -> {
            frameTimer.stop();
            process.destroy();

            replay = true;
            replayButton.setText("REPLAY");
        });

        frameTimer = new Timer(30, e -> {
            try {
                BufferedInputStream bufferedInputStream;
                BufferedImage frame = null;

                boolean isLoaded = false;

                while(!isLoaded) {
                    bufferedInputStream = new BufferedInputStream(process.getInputStream());
                    frame = ImageIO.read(bufferedInputStream);

                    if(frame != null) isLoaded = true;
                }

                frame = getGaussianBlur(frame);
                frame = motionTracking(frame);

                video.setIcon(new ImageIcon(frame));
                video.repaint();
            }

            catch(Exception exception) {
                System.out.println("Error: " + exception.getMessage());
            }
        });

        durationTimer.start();
        frameTimer.start();
    }

    private BufferedImage motionTracking(BufferedImage frame) {
        if(prevFrame == null) {
            prevFrame = frame;
            return frame;
        }

        int width = frame.getWidth();
        int height = frame.getHeight();

        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                int currentRGB = frame.getRGB(x, y);
                int prevRGB = prevFrame.getRGB(x, y);

                int rDifference = Math.abs(((currentRGB >> 16) & 0xFF) - ((prevRGB >> 16) & 0xFF));
                int gDifference = Math.abs(((currentRGB >> 8) & 0xFF) - ((prevRGB >> 8) & 0xFF));
                int bDifference = Math.abs((currentRGB & 0xFF) - (prevRGB & 0xFF));

                int combinedDifference = (int) Math.sqrt(rDifference * rDifference + gDifference * gDifference + bDifference * bDifference);

                if(combinedDifference > 30) mask.setRGB(x, y, 0xFFFFFF);
                else mask.setRGB(x, y, 0x000000);
            }
        }

        BufferedImage groupedMask = groupObjects ? groupPixels(mask) : mask;

        if(groupObjects) prevMask = groupedMask;
        else prevMask = null;

        BufferedImage highlightedFrame = overlayHighlight(frame, groupedMask);

        prevFrame = frame;

        return highlightedFrame;
    }

    private BufferedImage groupPixels(BufferedImage mask) {
        BufferedImage groupedMask = mask;

        int width = mask.getWidth();
        int height = mask.getHeight();

        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                if((mask.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
                    int key = maxKey;
                    if(prevMask != null && (prevMask.getRGB(x, y) & 0xFFFFFF) != 0) key = prevMask.getRGB(x, y);

                    floodFill(groupedMask, x, y, key);
                    if(key == maxKey) maxKey++;
                }
            }
        }

        return groupedMask;
    }

    private void floodFill(BufferedImage groupedMask, int x, int y, int key) {
        int width = groupedMask.getWidth();
        int height = groupedMask.getHeight();

        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));

        while(!stack.isEmpty()) {
            Point point = stack.pop();

            if(point.x < 0 || point.x >= width || point.y < 0 || point.y >= height || (groupedMask.getRGB(point.x, point.y) & 0xFFFFFF) != 0xFFFFFF) continue;

            int[][] neighbors = {
                    {point.x, point.y - 1},
                    {point.x + 1, point.y},
                    {point.x, point.y + 1},
                    {point.x - 1, point.y}
            };

            groupedMask.setRGB(point.x, point.y, key);

            for(int i = 0; i < neighbors.length; i++) {
                int neighborX = neighbors[i][0];
                int neighborY = neighbors[i][1];

                stack.push(new Point(neighborX, neighborY));
            }
        }

        if(key == maxKey) maxKey++;
    }

    private BufferedImage overlayHighlight(BufferedImage frame, BufferedImage mask) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        BufferedImage highlightedFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                int maskValue = mask.getRGB(x, y);

                if(maskValue != 0) {
                    Color groupColor = hashColor(maskValue);
                    int alphaGroupColor = new Color(groupColor.getRed(), groupColor.getGreen(), groupColor.getBlue(), 150).getRGB();

                    int rgb = frame.getRGB(x, y);
                    int green = (rgb >> 8) & 0xFF;
                    int blue = (rgb >> 16) & 0xFF;

                    highlightedFrame.setRGB(x, y, groupObjects ? blendColors(frame.getRGB(x, y), alphaGroupColor) : new Color(255, green, blue, 150).getRGB()); // blendColors(frame.getRGB(x, y), alphaGroupColor)
                } else {
                    highlightedFrame.setRGB(x, y, frame.getRGB(x, y));
                }
            }
        }

        return highlightedFrame;
    }

    private int blendColors(int baseColor, int overlayColor) {
        int baseAlpha = (baseColor >> 24) & 0xFF;
        int baseRed = (baseColor >> 16) & 0xFF;
        int baseGreen = (baseColor >> 8) & 0xFF;
        int baseBlue = baseColor & 0xFF;

        int overlayAlpha = (overlayColor >> 24) & 0xFF;
        int overlayRed = (overlayColor >> 16) & 0xFF;
        int overlayGreen = (overlayColor >> 8) & 0xFF;
        int overlayBlue = overlayColor & 0xFF;

        int outAlpha = Math.min(255, baseAlpha + overlayAlpha);
        int outRed = (baseRed * (255 - overlayAlpha) + overlayRed * overlayAlpha) / 255;
        int outGreen = (baseGreen * (255 - overlayAlpha) + overlayGreen * overlayAlpha) / 255;
        int outBlue = (baseBlue * (255 - overlayAlpha) + overlayBlue * overlayAlpha) / 255;

        return (outAlpha << 24) | (outRed << 16) | (outGreen << 8) | outBlue;
    }

    private Color hashColor(int key) {
        Random random = new Random(key);

        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);

        return new Color(r, g, b);
    }

    private BufferedImage getGaussianBlur(BufferedImage frame) {
        float[] kernel = {
                1f / 16, 2f / 16, 1f / 16,
                2f / 16, 4f / 16, 2f / 16,
                1f / 16, 2f / 16, 1f / 16
        };

        BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, kernel));
        return op.filter(frame, null);
    }
}
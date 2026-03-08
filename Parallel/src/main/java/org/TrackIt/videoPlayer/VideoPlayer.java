package org.TrackIt.videoPlayer;

import org.TrackIt.trackItSwing.TrackItButton;
import org.TrackIt.trackItSwing.TrackItVideo;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class VideoPlayer {
    // For parallel version, we will need these additional variables:

    // ForkJoinPool is used here instead of regular ThreadPoolExecutor.
    // ForkJoinPool allows work stealing, which results in better performance.
    private static final ForkJoinPool ForkJoinPool = new ForkJoinPool();

    public static final int SPLIT_THRESHOLD = 64; // Threshold below which we stop splitting the image.

    // For parallel version, Union-Find approach is used, instead of Flood-Fill.
    private AtomicIntegerArray parent;
    private AtomicIntegerArray rank;

    // Regular variables, just like in sequential version:
    private ProcessBuilder builder;
    private TrackItVideo video;
    private long duration;

    public boolean replay = false;
    public boolean groupObjects = false;

    private Timer durationTimer = null;
    private Timer frameTimer = null;

    private BufferedImage prevFrame = null;
    private BufferedImage prevMask  = null;

    public TrackItButton replayButton;

    public VideoPlayer() {
        replayButton = new TrackItButton("DETECTING MOVEMENT...");

        replayButton.onClick(e -> {
            if(replay) {
                try {
                    replay = false;
                    replayButton.setText("DETECTING MOVEMENT...");

                    display(builder, video, duration);
                }

                catch(Exception ex) {
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

                // Both of these are now parallel.
                frame = getGaussianBlur(frame);
                frame = motionTracking(frame);

                video.setIcon(new ImageIcon(frame));
                video.repaint();
            }

            catch(Exception ex) {
                System.out.println("Error: " + ex.getMessage());
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

        int[] currentPixels = frame.getRGB(0, 0, width, height, null, 0, width);
        int[] prevPixels = prevFrame.getRGB(0, 0, width, height, null, 0, width);

        // First phase: Computing pixel difference in order to check if movement was detected.
        int[] mask = new int[width * height];
        ForkJoinPool.invoke(new Difference(currentPixels, prevPixels, mask, width, height, 0, height));

        // Second phase: Connect groups from pixels.
        int[] roots = groupObjects ? connectGroups(mask, width, height) : mask;

        if(groupObjects) prevMask = buildImageFromRoots(roots, width, height);
        else prevMask = null;

        // Third phase: Drawing an image (frame) based on the collected data.
        int[] destPixels = new int[width * height];
        ForkJoinPool.invoke(new Draw(currentPixels, destPixels, roots, width, height, 0, height, groupObjects));

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, destPixels, 0, width);

        prevFrame = frame;
        return result;
    }

    private int[] connectGroups(int[] mask, int width, int height) {
        int n = width * height;

        parent = new AtomicIntegerArray(n);
        rank = new AtomicIntegerArray(n);

        for(int i = 0; i < n; i++) parent.set(i, i);

        // Pass 1
        ForkJoinPool.invoke(new Root(mask, parent, rank, width, height, 0, height));

        // Pass 2
        int[] roots = new int[n];
        ForkJoinPool.invoke(new Flatten(mask, parent, roots, width, height, 0, height));

        return remapRoots(roots, width, height);
    }

    private BufferedImage buildImageFromRoots(int[] roots, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, width, height, roots, 0, width);

        return img;
    }

    private int[] remapRoots(int[] roots, int width, int height) {
        if(prevMask == null) return roots;

        int[] prevRoots = new int[width * height];
        prevMask.getRGB(0, 0, width, height, prevRoots, 0, width);

        // We need to check pixel overlapping in order to find root's group from the previous frame (if exists).
        HashMap<Integer, Integer> rootMap = new HashMap<>();

        for(int i = 0; i < roots.length; i++) {
            if(roots[i] != 0 && prevRoots[i] != 0) rootMap.putIfAbsent(roots[i], prevRoots[i]);
        }

        int[] remapped = new int[roots.length];
        // If no group is found, then we will consider the root as it currently is.
        for(int i = 0; i < roots.length; i++) remapped[i] = roots[i] == 0 ? 0 : rootMap.getOrDefault(roots[i], roots[i]);

        return remapped;
    }

    // Parallel version of getGaussianBlur method.
    // Each recursive task handles a horizontal strip [yStart, yEnd).
    // Strips are split in half until they're smaller than SPLIT_THRESHOLD.
    private BufferedImage getGaussianBlur(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        // The idea is to pre-read all source pixels once, there is no need to read them every time.
        int[] srcPixels = src.getRGB(0, 0, width, height, null, 0, width);
        int[] destPixels = new int[srcPixels.length];

        ForkJoinPool.invoke(new Blur(srcPixels, destPixels, width, height, 0, height));

        BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        dest.setRGB(0, 0, width, height, destPixels, 0, width);

        return dest;
    }
}
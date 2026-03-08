package org.TrackIt.videoPlayer;

import mpi.*;
import org.TrackIt.trackItSwing.TrackItButton;
import org.TrackIt.trackItSwing.TrackItVideo;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class VideoPlayer {
    // For distributed version, we will need these additional variables:

    // ForkJoinPool is used here instead of regular ThreadPoolExecutor.
    // ForkJoinPool allows work stealing, which results in better performance.
    private static final ForkJoinPool ForkJoinPool = new ForkJoinPool();

    public static final int SPLIT_THRESHOLD = 64; // Threshold below which we stop splitting the image.

    private static final int MASTER = 0; // The root process of MPI.

    // The following variables are used only by Master for dealing with UI.
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

    // Constructor is also used only by Master.
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

    // This method is used by all workers (processes that are not a Master process).
    // Explanation of what each worker does:
    // 1. Receives its strip of currentPixels and prevPixels.
    // 2. Locally runs Difference, which is parallelly distributed via ForkJoin.
    // 3. In case of grouping, worker runs Root locally, then sends mask back so Master can merge borders, and then worker receives merged parent array, it runs Flatten, and sends roots back.
    // 4. Worker receives roots, runs Draw locally, and sends destPixels back.
    public static void workerLoop() throws MPIException {
        while(true) {
            // Incoming messages are either some working tag or TERMINATE.
            Status status = MPI.COMM_WORLD.Probe(MASTER, MPI.ANY_TAG);

            if(status.tag == Tags.TERMINATE) {
                // Here we will create one simple array just to collect the message, but since its TERMINATE, it's not important.
                int[] garbage = new int[1];
                MPI.COMM_WORLD.Recv(garbage, 0, 1, MPI.INT, MASTER, Tags.TERMINATE);

                return;
            }

            // In case the tag wasn't TERMINATE, then data exists and is written in following order: [width, height, stripStart, stripEnd, groupObjects].
            int[] data = new int[5];
            MPI.COMM_WORLD.Recv(data, 0, 5, MPI.INT, MASTER, Tags.DATA);

            int width = data[0];
            int stripStart = data[2];
            int stripEnd = data[3];

            boolean grouped = data[4] == 1;

            int stripSize = (stripEnd - stripStart) * width;

            // Variables of strips to be received.
            int[] currentStrip = new int[stripSize];
            int[] prevStrip = new int[stripSize];

            MPI.COMM_WORLD.Recv(currentStrip, 0, stripSize, MPI.INT, MASTER, Tags.CURRENT);
            MPI.COMM_WORLD.Recv(prevStrip, 0, stripSize, MPI.INT, MASTER, Tags.PREV);

            // First phase: Computing pixel difference in order to check if movement was detected.
            int[] mask = new int[stripSize];
            ForkJoinPool.invoke(new Difference(currentStrip, prevStrip, mask, width, stripEnd - stripStart, 0, stripEnd - stripStart));

            // Second phase: Connect groups from pixels.
            int[] roots;

            if(grouped) {
                // Send mask back to Master for border adjustments.
                MPI.COMM_WORLD.Send(mask, 0, stripSize, MPI.INT, MASTER, Tags.MASK);

                // Master sends resolved roots slice, unlike parallel version there is no need for Flatten here.
                roots = new int[stripSize];
                MPI.COMM_WORLD.Recv(roots, 0, stripSize, MPI.INT, MASTER, Tags.ROOTS);
            }

            else roots = mask;

            // Third phase: Drawing an image (frame) based on the collected data.
            int[] destStrip = new int[stripSize];
            ForkJoinPool.invoke(new Draw(currentStrip, destStrip, roots, width, stripEnd - stripStart, 0, stripEnd - stripStart, grouped));

            MPI.COMM_WORLD.Send(destStrip, 0, stripSize, MPI.INT, MASTER, Tags.DEST);
        }
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

            // In distributed version, we need to terminate all the workers, as well.
            try {
                int size = MPI.COMM_WORLD.Size();

                int[] garbage = new int[]{0};
                for (int r = 1; r < size; r++) MPI.COMM_WORLD.Send(garbage, 0, 1, MPI.INT, r, Tags.TERMINATE);
            }

            catch(MPIException ex) {
                System.out.println("Shutdown error: " + ex.getMessage());
            }
        });

        frameTimer = new Timer(30, e -> {
            try {
                BufferedInputStream bufferedInputStream;
                BufferedImage frame = null;

                while(frame == null) {
                    bufferedInputStream = new BufferedInputStream(process.getInputStream());
                    frame = ImageIO.read(bufferedInputStream);
                }

                // Both of these are now parallel.
                frame = getGaussianBlur(frame);
                frame = motionTracking(frame);

                video.setIcon(new ImageIcon(frame));
                video.repaint();
            }

            catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        });

        durationTimer.start();
        frameTimer.start();
    }

    // For this method Master is used to distribute work across workers.
    private BufferedImage motionTracking(BufferedImage frame) throws MPIException {
        if(prevFrame == null) {
            prevFrame = frame;
            return frame;
        }

        int width = frame.getWidth();
        int height = frame.getHeight();

        int[] currentPixels = frame.getRGB(0, 0, width, height, null, 0, width);
        int[] prevPixels = prevFrame.getRGB(0, 0, width, height, null, 0, width);

        int size = MPI.COMM_WORLD.Size(); // Holds total number of processes (Master and workers).

        // Dividing frame into strips, one per process.
        int[] stripStarts = new int[size];
        int[] stripEnds = new int[size];

        int rowsPerNode = height / size;

        for(int r = 0; r < size; r++) {
            stripStarts[r] = r * rowsPerNode;
            stripEnds[r] = (r == size - 1) ? height : stripStarts[r] + rowsPerNode;
        }

        // Sending strips to workers.
        for(int r = 1; r < size; r++) {
            int start = stripStarts[r];
            int end = stripEnds[r];

            int stripSize = (end - start) * width;

            int[] data = {width, height, start, end, groupObjects ? 1 : 0};
            MPI.COMM_WORLD.Send(data, 0, 5, MPI.INT, r, Tags.DATA);

            // Here we introduce the offset of start * width.
            MPI.COMM_WORLD.Send(currentPixels, start * width, stripSize, MPI.INT, r, Tags.CURRENT);
            MPI.COMM_WORLD.Send(prevPixels, start * width, stripSize, MPI.INT, r, Tags.PREV);
        }

        // Master also processes its own strip locally.
        int masterStart = stripStarts[MASTER];
        int masterEnd = stripEnds[MASTER];

        int masterSize = (masterEnd - masterStart) * width;

        int[] masterCurrent = new int[masterSize];
        int[] masterPrev = new int[masterSize];

        System.arraycopy(currentPixels, masterStart * width, masterCurrent, 0, masterSize);
        System.arraycopy(prevPixels, masterStart * width, masterPrev, 0, masterSize);

        // First phase: Computing pixel difference in order to check if movement was detected.
        int[] masterMask = new int[masterSize];
        ForkJoinPool.invoke(new Difference(masterCurrent, masterPrev, masterMask, width, masterEnd - masterStart, 0, masterEnd - masterStart));

        // Second phase: Connect groups from pixels.
        int[] fullMask = new int[width * height];
        System.arraycopy(masterMask, 0, fullMask, masterStart * width, masterSize);

        if(groupObjects) {
            // Collecting masks from workers.
            for(int r = 1; r < size; r++) {
                int start = stripStarts[r];
                int stripSize = (stripEnds[r] - start) * width;

                int[] workerMask = new int[stripSize];
                MPI.COMM_WORLD.Recv(workerMask, 0, stripSize, MPI.INT, r, Tags.MASK);

                System.arraycopy(workerMask, 0, fullMask, start * width, stripSize);
            }

            // Performing UF on Master process.
            int n = width * height;

            AtomicIntegerArray parent = new AtomicIntegerArray(n);
            AtomicIntegerArray rank = new AtomicIntegerArray(n);

            for(int i = 0; i < n; i++) parent.set(i, i);

            ForkJoinPool.invoke(new Root(fullMask, parent, rank, width, height, 0, height));

            // Adjusting strip borders.
            for(int r = 1; r < size; r++) {
                UF uf = new UF(parent, rank);
                int borderY = stripStarts[r];

                for(int x = 0; x < width; x++) {
                    int above = (borderY - 1) * width + x;
                    int below = borderY * width + x;

                    if(fullMask[above] == 1 && fullMask[below] == 1) uf.union(above, below);
                }
            }

            // Flattening the complete parent array.
            int[] fullRoots = new int[n];
            ForkJoinPool.invoke(new Flatten(fullMask, parent, fullRoots, width, height, 0, height));

            fullRoots = remapRoots(fullRoots, width, height);
            prevMask = buildImageFromRoots(fullRoots, width, height);

            // Sending each worker its resolved roots slice.
            for(int r = 1; r < size; r++) {
                int start = stripStarts[r];
                int stripSize = (stripEnds[r] - start) * width;

                MPI.COMM_WORLD.Send(fullRoots, start * width, stripSize, MPI.INT, r, Tags.ROOTS);
            }

            // Master draws its own strip.
            int[] masterRootsSlice = new int[masterSize];
            System.arraycopy(fullRoots, masterStart * width, masterRootsSlice, 0, masterSize);

            int[] masterDest = new int[masterSize];
            ForkJoinPool.invoke(new Draw(masterCurrent, masterDest, masterRootsSlice, width, masterEnd - masterStart, 0, masterEnd - masterStart, true));

            // Collect drawn strips from workers.
            int[] destPixels = new int[width * height];
            System.arraycopy(masterDest, 0, destPixels, masterStart * width, masterSize);

            for(int r = 1; r < size; r++) {
                int start = stripStarts[r];
                int stripSize = (stripEnds[r] - start) * width;

                int[] workerDest = new int[stripSize];
                MPI.COMM_WORLD.Recv(workerDest, 0, stripSize, MPI.INT, r, Tags.DEST);

                System.arraycopy(workerDest, 0, destPixels, start * width, stripSize);
            }

            prevFrame = frame;

            BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            result.setRGB(0, 0, width, height, destPixels, 0, width);

            return result;

        }

        else {
            prevMask = null;

            int[] masterDest = new int[masterSize];
            ForkJoinPool.invoke(new Draw(masterCurrent, masterDest, masterMask, width, masterEnd - masterStart, 0, masterEnd - masterStart, false));

            int[] destPixels = new int[width * height];
            System.arraycopy(masterDest, 0, destPixels, masterStart * width, masterSize);

            for(int r = 1; r < size; r++) {
                int start = stripStarts[r];
                int stripSize = (stripEnds[r] - start) * width;

                int[] workerDest = new int[stripSize];
                MPI.COMM_WORLD.Recv(workerDest, 0, stripSize, MPI.INT, r, Tags.DEST);

                System.arraycopy(workerDest, 0, destPixels, start * width, stripSize);
            }

            prevFrame = frame;

            BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            result.setRGB(0, 0, width, height, destPixels, 0, width);

            return result;
        }
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
        for (int i = 0; i < roots.length; i++) remapped[i] = roots[i] == 0 ? 0 : rootMap.getOrDefault(roots[i], roots[i]);

        return remapped;
    }

    // Each recursive task handles a horizontal strip [yStart, yEnd).
    // Strips are split in half until they're smaller than SPLIT_THRESHOLD.
    private BufferedImage getGaussianBlur(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        // The idea is to pre-read all source pixels once, there is no need to read them every time.
        int[] srcPixels  = src.getRGB(0, 0, width, height, null, 0, width);
        int[] destPixels = new int[srcPixels.length];

        ForkJoinPool.invoke(new Blur(srcPixels, destPixels, width, height, 0, height));

        BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        dest.setRGB(0, 0, width, height, destPixels, 0, width);

        return dest;
    }
}
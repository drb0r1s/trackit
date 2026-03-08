package org.TrackIt.videoPlayer;

public class Tags {
    public static final int CURRENT = 0; // Pixels of the current frame.
    public static final int PREV = 1; // Pixels of the previous frame.
    public static final int MASK = 2; // Mask of difference pixels.
    public static final int ROOTS = 3; // Root pixels.
    public static final int DEST = 4; // Drawn pixels.
    public static final int DATA = 5; // Additional data (width, height, flags).
    public static final int TERMINATE = 6; // Signal for workers to stop.
}
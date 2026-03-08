package org.TrackIt.videoPlayer;

import java.util.concurrent.RecursiveAction;

public class Difference extends RecursiveAction {
    private static final int MOTION_THRESHOLD = 30; // Threshold for motion-detection, the same value is used as in sequential version.

    private final int[] current, prev, mask;
    private final int width, height, yStart, yEnd;

    Difference(int[] current, int[] prev, int[] mask, int width, int height, int yStart, int yEnd) {
        this.current = current;
        this.prev = prev;
        this.mask = mask;

        this.width = width;
        this.height = height;

        this.yStart = yStart;
        this.yEnd = yEnd;
    }

    @Override
    protected void compute() {
        if(yEnd - yStart <= VideoPlayer.SPLIT_THRESHOLD) {
            computeDirectly();
            return;
        }

        int mid = (yStart + yEnd) / 2;

        invokeAll(
                new Difference(current, prev, mask, width, height, yStart, mid),
                new Difference(current, prev, mask, width, height, mid, yEnd)
        );
    }

    private void computeDirectly() {
        for(int y = yStart; y < yEnd; y++) {
            for(int x = 0; x < width; x++) {
                int pixel = y * width + x;

                int currentPixel = current[pixel];
                int prevPixel = prev[pixel];

                int differenceR = Math.abs(((currentPixel >> 16) & 0xFF) - ((prevPixel >> 16) & 0xFF));
                int differenceG = Math.abs(((currentPixel >>  8) & 0xFF) - ((prevPixel >>  8) & 0xFF));
                int differenceB = Math.abs((currentPixel & 0xFF) - (prevPixel & 0xFF));

                int difference = (int) Math.sqrt(differenceR * differenceR + differenceG * differenceG + differenceB * differenceB);
                mask[pixel] = difference > MOTION_THRESHOLD ? 1 : 0;
            }
        }
    }
}

package org.TrackIt.videoPlayer;

import java.util.concurrent.RecursiveAction;

public class Blur extends RecursiveAction {
    private static final float[] kernel = {
            1f/16, 2f/16, 1f/16,
            2f/16, 4f/16, 2f/16,
            1f/16, 2f/16, 1f/16
    };

    private final int[] src, dest;
    private final int width, height, yStart, yEnd;

    Blur(int[] src, int[] dest, int width, int height, int yStart, int yEnd) {
        this.src = src;
        this.dest = dest;

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
                new Blur(src, dest, width, height, yStart, mid),
                new Blur(src, dest, width, height, mid, yEnd)
        );
    }

    // Sequential version is using ConvolveOp for blur.
    // However, for parallel version, manual implementation was needed, in order to achieve proper parallelization.
    private void computeDirectly() {
        for(int y = yStart; y < yEnd; y++) {
            for(int x = 0; x < width; x++) {
                float a = 0, r = 0, g = 0, b = 0;

                for(int ky = -1; ky <= 1; ky++) {
                    for(int kx = -1; kx <= 1; kx++) {
                        int nx = Math.min(Math.max(x + kx, 0), width - 1);
                        int ny = Math.min(Math.max(y + ky, 0), height - 1);

                        int px = src[ny * width + nx];

                        float weight = kernel[(ky + 1) * 3 + (kx + 1)];

                        a += ((px >> 24) & 0xFF) * weight;
                        r += ((px >> 16) & 0xFF) * weight;
                        g += ((px >>  8) & 0xFF) * weight;
                        b += (px & 0xFF) * weight;
                    }
                }

                dest[y * width + x] = ((int) a << 24) | ((int) r << 16) | ((int) g << 8) | (int) b;
            }
        }
    }
}

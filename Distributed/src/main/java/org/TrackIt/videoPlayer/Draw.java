package org.TrackIt.videoPlayer;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.RecursiveAction;

public class Draw extends RecursiveAction {
    private final int[] src, dest, roots;
    private final int width, height, yStart, yEnd;
    private final boolean grouped;

    Draw(int[] src, int[] dest, int[] roots, int width, int height, int yStart, int yEnd, boolean grouped) {
        this.src = src;
        this.dest = dest;

        this.roots = roots;

        this.width = width;
        this.height = height;

        this.yStart = yStart;
        this.yEnd = yEnd;

        this.grouped = grouped;
    }

    @Override
    protected void compute() {
        if(yEnd - yStart <= VideoPlayer.SPLIT_THRESHOLD) {
            computeDirectly();
            return;
        }

        int mid = (yStart + yEnd) / 2;

        invokeAll(
                new Draw(src, dest, roots, width, height, yStart, mid, grouped),
                new Draw(src, dest, roots, width, height, mid, yEnd, grouped)
        );
    }

    private void computeDirectly() {
        for(int y = yStart; y < yEnd; y++) {
            for(int x = 0; x < width; x++) {
                int pixel = y * width + x;
                int root = roots[pixel];
                int px = src[pixel];

                if(root != 0) {
                    if(grouped) {
                        Color groupColor = hashColor(root);
                        int mask = (150 << 24) | (groupColor.getRed() << 16) | (groupColor.getGreen() << 8) | groupColor.getBlue();
                        dest[pixel] = blendColors(px, mask);
                    }

                    else {
                        // Original color for non-grouped pixels.
                        int green = (px >> 8) & 0xFF;
                        int blue = (px >> 16) & 0xFF;

                        dest[pixel] = (150 << 24) | (255 << 16) | (green << 8) | blue;
                    }
                }

                else dest[pixel] = px;
            }
        }
    }

    private static Color hashColor(int key) {
        Random rng = new Random(key);
        return new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
    }

    private static int blendColors(int base, int mask) {
        int bA = (base >> 24) & 0xFF, bR = (base >> 16) & 0xFF, bG = (base >> 8) & 0xFF, bB = base & 0xFF;
        int oA = (mask >> 24) & 0xFF, oR = (mask >> 16) & 0xFF, oG = (mask >> 8) & 0xFF, oB = mask & 0xFF;

        int rA = Math.min(255, bA + oA);
        int rR = (bR * (255 - oA) + oR * oA) / 255;
        int rG = (bG * (255 - oA) + oG * oA) / 255;
        int rB = (bB * (255 - oA) + oB * oA) / 255;

        return (rA << 24) | (rR << 16) | (rG << 8) | rB;
    }
}

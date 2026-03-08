package org.TrackIt.videoPlayer;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class Flatten extends RecursiveAction {
    private final int[] mask, roots;

    private final AtomicIntegerArray parent;
    private final UF uf;

    private final int width, height, yStart, yEnd;

    Flatten(int[] mask, AtomicIntegerArray parent, int[] roots, int width, int height, int yStart, int yEnd) {
        this.mask = mask;

        this.parent = parent;
        this.uf = new UF(parent);

        this.roots = roots;

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
                new Flatten(mask, parent, roots, width, height, yStart, mid),
                new Flatten(mask, parent, roots, width, height, mid, yEnd)
        );
    }

    private void computeDirectly() {
        for(int y = yStart; y < yEnd; y++) {
            for(int x = 0; x < width; x++) {
                int pixel = y * width + x;
                roots[pixel] = mask[pixel] == 1 ? uf.find(pixel) + 1 : 0;
            }
        }
    }
}

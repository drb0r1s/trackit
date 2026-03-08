package org.TrackIt.videoPlayer;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class Root extends RecursiveAction {
    private final int[] mask;

    private final AtomicIntegerArray parent;
    private final AtomicIntegerArray rank;

    private final int width, height, yStart, yEnd;

    private final UF uf;

    Root(int[] mask, AtomicIntegerArray parent, AtomicIntegerArray rank, int width, int height, int yStart, int yEnd) {
        this.mask = mask;

        this.parent = parent;
        this.rank = rank;

        this.uf = new UF(parent, rank);

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
                new Root(mask, parent, rank, width, height, yStart, mid),
                new Root(mask, parent, rank, width, height, mid, yEnd)
        );
    }

    private void computeDirectly() {
        for(int y = yStart; y < yEnd; y++) {
            for(int x = 0; x < width; x++) {
                int pixel = y * width + x;

                if(mask[pixel] == 0) continue;

                if(x > 0 && mask[pixel - 1] == 1) uf.union(pixel, pixel - 1);
                if(y > 0 && mask[pixel - width] == 1) uf.union(pixel, pixel - width);
            }
        }
    }
}

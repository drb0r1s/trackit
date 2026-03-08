package org.TrackIt.videoPlayer;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class UF {
    private final AtomicIntegerArray parent;
    private final AtomicIntegerArray rank;

    UF(AtomicIntegerArray parent, AtomicIntegerArray rank) {
        this.parent = parent;
        this.rank = rank;
    }

    UF(AtomicIntegerArray parent) {
        this.parent = parent;
        this.rank = null;
    }

    public void union(int a, int b) {
        // If union failed or another thread already modified b's parent, retry happens.
        while(true) {
            a = find(a);
            b = find(b);

            if(a == b) return;

            int ra = rank.get(a);
            int rb = rank.get(b);

            // Always try to attach the lower-rank root under the higher-rank one.
            if(ra < rb) {
                int t = a;

                a = b;
                b = t;
            }

            // Only set parent[b] = a if it still points to itself.
            if(parent.compareAndSet(b, b, a)) {
                if(ra == rb) rank.compareAndSet(a, ra, ra + 1);
                return;
            }
        }
    }

    public int find(int i) {
        while(true) {
            int p = parent.get(i);
            if (p == i) return i;

            int gp = parent.get(p);
            parent.compareAndSet(i, p, gp);

            i = gp;
        }
    }
}

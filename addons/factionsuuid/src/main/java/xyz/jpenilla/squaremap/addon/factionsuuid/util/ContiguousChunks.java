package xyz.jpenilla.squaremap.addon.factionsuuid.util;

import com.massivecraft.factions.FLocation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ContiguousChunks {

    private static final int[][] NEIGHBORS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private ContiguousChunks() {
    }

    /**
     * Splits chunk locations into groups that are 4-connected (edge-adjacent in the chunk grid).
     */
    public static List<List<FLocation>> group(final Collection<FLocation> claims) {
        final Set<FLocation> unvisited = new HashSet<>(claims);
        final List<List<FLocation>> groups = new ArrayList<>();
        while (!unvisited.isEmpty()) {
            final FLocation start = unvisited.iterator().next();
            final List<FLocation> component = new ArrayList<>();
            final ArrayDeque<FLocation> queue = new ArrayDeque<>();
            unvisited.remove(start);
            queue.add(start);
            component.add(start);
            while (!queue.isEmpty()) {
                final FLocation cur = queue.removeFirst();
                for (final int[] d : NEIGHBORS) {
                    final FLocation n = cur.getRelative(d[0], d[1]);
                    if (unvisited.remove(n)) {
                        component.add(n);
                        queue.add(n);
                    }
                }
            }
            groups.add(component);
        }
        return groups;
    }
}

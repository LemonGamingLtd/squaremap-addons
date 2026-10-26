package xyz.jpenilla.squaremap.addon.factionsuuid.util;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.Polygon;

// Same approach as claimchunk RectangleMerge — union chunk squares into one outline.

public final class ChunkPolygonMerge {

    private ChunkPolygonMerge() {
    }

    public static Polygon toPolygon(final List<int[]> chunkCoords) {
        final List<Shape> shapes = new ArrayList<>(chunkCoords.size());
        for (final int[] c : chunkCoords) {
            final int x = c[0] << 4;
            final int z = c[1] << 4;
            final List<Point> points = Arrays.asList(
                Point.of(x, z),
                Point.of(x, z + 16),
                Point.of(x + 16, z + 16),
                Point.of(x + 16, z)
            );
            shapes.add(toShape(points));
        }

        Area area = null;
        for (final Shape shape : shapes) {
            if (area == null) {
                area = new Area(shape);
            } else {
                area.add(new Area(shape));
            }
        }

        if (area == null) {
            throw new IllegalStateException();
        }

        return toMarker(area);
    }

    private static Shape toShape(final List<Point> points) {
        final Path2D path = new Path2D.Double();
        for (int i = 0; i < points.size(); i++) {
            final Point p = points.get(i);
            if (i == 0) {
                path.moveTo(p.x(), p.z());
            } else {
                path.lineTo(p.x(), p.z());
            }
        }
        path.closePath();
        return path;
    }

    private static Polygon toMarker(final Shape shape) {
        final List<Point> main = new ArrayList<>();
        final List<List<Point>> subtract = new ArrayList<>();
        final PathIterator iter = shape.getPathIterator(null, 0.0);
        final double[] coords = new double[6];
        List<Point> current = main;
        while (!iter.isDone()) {
            final int segment = iter.currentSegment(coords);
            switch (segment) {
                case PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> current.add(Point.of(coords[0], coords[1]));
                case PathIterator.SEG_CLOSE -> {
                    final List<Point> newList = new ArrayList<>();
                    subtract.add(newList);
                    current = newList;
                }
            }
            iter.next();
        }
        subtract.removeIf(List::isEmpty);
        return Marker.polygon(main, subtract);
    }
}

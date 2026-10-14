package xyz.jpenilla.squaremap.addon.coreprotect.hook;

import org.bukkit.World;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Squaremap;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Queues a chunk for squaremap's background render.
 */
public final class SquaremapChunkMarker {
    private static final String MAP_WORLD_INTERNAL_LEGACY = "xyz.jpenilla.squaremap.common.data.MapWorldInternal";
    private static final String CHUNK_COORDINATE_LEGACY = "xyz.jpenilla.squaremap.common.data.ChunkCoordinate";

    private static final String MAP_WORLD_INTERNAL = "xyz.jpenilla.squaremap.common.world.MapWorldInternal";
    private static final String CHUNK_COORDINATE = "xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate";

    private final Squaremap squaremap;
    private final Class<?> mapWorldInternalClass;
    private final MethodHandle newChunkCoordinate;
    private final MethodHandle chunkModified;

    public SquaremapChunkMarker(final Squaremap squaremap) throws ReflectiveOperationException {
        Class<?> mapWorldInternalClassDef;
        this.squaremap = squaremap;

        final ClassLoader loader = squaremap.getClass().getClassLoader();
        try {
            mapWorldInternalClassDef = Class.forName(MAP_WORLD_INTERNAL, true, loader);
        } catch (final ClassNotFoundException ex) {
            mapWorldInternalClassDef = Class.forName(MAP_WORLD_INTERNAL_LEGACY, true, loader);
        }
        this.mapWorldInternalClass = mapWorldInternalClassDef;

        Class<?> chunkCoordinateClass;
        try {
            chunkCoordinateClass = Class.forName(CHUNK_COORDINATE, true, loader);
        } catch (final ClassNotFoundException ex) {
            chunkCoordinateClass = Class.forName(CHUNK_COORDINATE_LEGACY, true, loader);
        }

        final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        this.newChunkCoordinate = lookup.findConstructor(chunkCoordinateClass, MethodType.methodType(void.class, int.class, int.class));
        this.chunkModified = lookup.findVirtual(this.mapWorldInternalClass, "chunkModified", MethodType.methodType(void.class, chunkCoordinateClass));
    }

    public void markModified(final World world, final int chunkX, final int chunkZ) throws Throwable {
        final MapWorld mapWorld = this.squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).orElse(null);
        if (mapWorld == null || !this.mapWorldInternalClass.isInstance(mapWorld)) {
            return;
        }
        this.chunkModified.invoke(mapWorld, this.newChunkCoordinate.invoke(chunkX, chunkZ));
    }
}

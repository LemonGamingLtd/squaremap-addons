package xyz.jpenilla.squaremap.addon.coreprotect.hook;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.bukkit.World;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Squaremap;

/**
 * Queues a chunk for squaremap's background render.
 */
public final class SquaremapChunkMarker {
    private static final String MAP_WORLD_INTERNAL = "xyz.jpenilla.squaremap.common.data.MapWorldInternal";
    private static final String CHUNK_COORDINATE = "xyz.jpenilla.squaremap.common.data.ChunkCoordinate";

    private final Squaremap squaremap;
    private final Class<?> mapWorldInternalClass;
    private final MethodHandle newChunkCoordinate;
    private final MethodHandle chunkModified;

    public SquaremapChunkMarker(final Squaremap squaremap) throws ReflectiveOperationException {
        this.squaremap = squaremap;
        final ClassLoader loader = squaremap.getClass().getClassLoader();
        this.mapWorldInternalClass = Class.forName(MAP_WORLD_INTERNAL, true, loader);
        final Class<?> chunkCoordinateClass = Class.forName(CHUNK_COORDINATE, true, loader);
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

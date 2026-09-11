package xyz.jpenilla.squaremap.addon.coreprotect.listener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.jpenilla.squaremap.addon.coreprotect.hook.SquaremapChunkMarker;

/**
 * Listens for {@code net.coreprotect.event.CoreProtectRollbackEvent} reflectively.
 */
public final class CoreProtectRollbackListener implements Listener {
    private static final String EVENT_CLASS = "net.coreprotect.event.CoreProtectRollbackEvent";

    private final JavaPlugin plugin;
    private final SquaremapChunkMarker chunkMarker;
    private final Class<? extends Event> eventClass;
    private final MethodHandle getWorld;
    private final MethodHandle getChunkX;
    private final MethodHandle getChunkZ;

    public CoreProtectRollbackListener(final JavaPlugin plugin, final SquaremapChunkMarker chunkMarker) throws ReflectiveOperationException {
        this.plugin = plugin;
        this.chunkMarker = chunkMarker;
        this.eventClass = Class.forName(EVENT_CLASS).asSubclass(Event.class);
        final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        this.getWorld = lookup.findVirtual(this.eventClass, "getWorld", MethodType.methodType(World.class));
        this.getChunkX = lookup.findVirtual(this.eventClass, "getChunkX", MethodType.methodType(int.class));
        this.getChunkZ = lookup.findVirtual(this.eventClass, "getChunkZ", MethodType.methodType(int.class));
    }

    public void register() {
        this.plugin.getServer().getPluginManager().registerEvent(
            this.eventClass,
            this,
            EventPriority.MONITOR,
            (listener, event) -> this.handle(event),
            this.plugin,
            true
        );
    }

    private void handle(final Event event) {
        if (!this.eventClass.isInstance(event)) {
            return;
        }
        try {
            final World world = (World) this.getWorld.invoke(event);
            final int chunkX = (int) this.getChunkX.invoke(event);
            final int chunkZ = (int) this.getChunkZ.invoke(event);
            this.chunkMarker.markModified(world, chunkX, chunkZ);
        } catch (final Throwable ex) {
            this.plugin.getLogger().warning("Failed to queue map update for CoreProtect rollback: " + ex);
        }
    }
}

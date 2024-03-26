package xyz.jpenilla.squaremap.addon.factionsuuid.hook;

import xyz.jpenilla.squaremap.addon.factionsuuid.SquaremapFactions;
import xyz.jpenilla.squaremap.addon.factionsuuid.task.SquaremapTask;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class SquaremapHook {
    private static final Key FACTIONS_LAYER_KEY = Key.of("factions");

    private final Map<WorldIdentifier, SquaremapTask> tasks = new HashMap<>();

    public SquaremapHook(SquaremapFactions plugin) {
        for (final MapWorld world : SquaremapProvider.get().mapWorlds()) {
            final SimpleLayerProvider provider = SimpleLayerProvider
                .builder(plugin.config().controlLabel)
                .showControls(plugin.config().controlShow)
                .defaultHidden(plugin.config().controlHide)
                .zIndex(plugin.config().zIndex)
                .layerPriority(plugin.config().layerPriority)
                .build();
            world.layerRegistry().register(FACTIONS_LAYER_KEY, provider);

            final SquaremapTask task = new SquaremapTask(plugin, world, provider);
            task.runTaskTimerAsynchronously(plugin.scheduler(), 1L, plugin.config().updateInterval, TimeUnit.SECONDS);

            this.tasks.put(world.identifier(), task);
        }
    }

    public void disable() {
        this.tasks.values().forEach(SquaremapTask::disable);
        this.tasks.clear();
    }
}

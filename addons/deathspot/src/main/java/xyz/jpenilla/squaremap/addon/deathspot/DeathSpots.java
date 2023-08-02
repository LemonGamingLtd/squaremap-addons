package xyz.jpenilla.squaremap.addon.deathspot;

import org.bukkit.Location;
import xyz.jpenilla.squaremap.addon.common.AddonJavaPlugin;
import xyz.jpenilla.squaremap.addon.deathspot.config.DeathSpotConfig;
import xyz.jpenilla.squaremap.addon.deathspot.hook.SquaremapHook;
import xyz.jpenilla.squaremap.addon.deathspot.listener.PlayerListener;
import xyz.jpenilla.squaremap.api.Pair;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeathSpots extends AddonJavaPlugin {
    private static DeathSpots instance;
    private DeathSpotConfig config;
    private final Map<UUID, Pair<String, Location>> deathSpots = new HashMap<>();
    private SquaremapHook squaremapHook;

    public DeathSpots() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.config = new DeathSpotConfig(this);
        this.config.reload();

        if (!new File(this.getDataFolder(), "icon.png").exists()) {
            this.saveResource("icon.png", false);
        }

        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        this.squaremapHook = new SquaremapHook(this);
    }

    @Override
    public void onDisable() {
        if (this.squaremapHook != null) {
            this.squaremapHook.disable();
        }
        this.deathSpots.clear();
        scheduler().getImpl().cancelAllTasks();
    }

    public static DeathSpots getInstance() {
        return instance;
    }

    public Map<UUID, Pair<String, Location>> getDeathSpots() {
        return this.deathSpots;
    }

    public DeathSpotConfig config() {
        return this.config;
    }
}

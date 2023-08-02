package xyz.jpenilla.squaremap.addon.common;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.jpenilla.squaremap.addon.common.util.LazyValue;

/**
 * Abstract wrapper for addon plugins.
 */
public abstract class AddonJavaPlugin extends JavaPlugin {

    private final LazyValue<FoliaLib> scheduler = new LazyValue<>(() -> new FoliaLib(this));

    /**
     * Get the scheduler for this addon plugin.
     *
     * @return {@link FoliaLib} scheduler.
     */
    public FoliaLib scheduler() {
        return scheduler.getValue();
    }
}

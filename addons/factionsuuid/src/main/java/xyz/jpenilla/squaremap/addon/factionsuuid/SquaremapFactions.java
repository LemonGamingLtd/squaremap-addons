package xyz.jpenilla.squaremap.addon.factionsuuid;

import xyz.jpenilla.squaremap.addon.common.AddonJavaPlugin;
import xyz.jpenilla.squaremap.addon.factionsuuid.config.FactionsConfig;
import xyz.jpenilla.squaremap.addon.factionsuuid.hook.SquaremapHook;
import xyz.jpenilla.squaremap.addon.factionsuuid.web.FactionSearch;

public final class SquaremapFactions extends AddonJavaPlugin {
    private SquaremapHook squaremapHook;
    private FactionsConfig config;
    private FactionSearch factionSearch;

    public FactionsConfig config() {
        return this.config;
    }

    public FactionSearch factionSearch() {
        return this.factionSearch;
    }

    @Override
    public void onEnable() {
        this.config = new FactionsConfig(this);
        this.config.reload();
        this.factionSearch = new FactionSearch(this);

        this.squaremapHook = new SquaremapHook(this);
    }

    @Override
    public void onDisable() {
        if (this.squaremapHook != null) {
            this.squaremapHook.disable();
        }
        scheduler().getImpl().cancelAllTasks();
    }
}

package xyz.jpenilla.squaremap.addon.griefprevention;

import xyz.jpenilla.squaremap.addon.common.AddonJavaPlugin;
import xyz.jpenilla.squaremap.addon.griefprevention.config.GPConfig;
import xyz.jpenilla.squaremap.addon.griefprevention.hook.SquaremapHook;

public final class SquaremapGriefPrevention extends AddonJavaPlugin {
    private SquaremapHook squaremapHook;
    private GPConfig config;

    public GPConfig config() {
        return this.config;
    }

    @Override
    public void onEnable() {
        this.config = new GPConfig(this);
        this.config.reload();

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

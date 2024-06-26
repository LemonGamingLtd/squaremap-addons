package xyz.jpenilla.squaremap.addon.mobs;

import xyz.jpenilla.squaremap.addon.common.AddonJavaPlugin;
import xyz.jpenilla.squaremap.addon.mobs.config.MobsConfig;
import xyz.jpenilla.squaremap.addon.mobs.data.Icons;
import xyz.jpenilla.squaremap.addon.mobs.hook.SquaremapHook;

public final class SquaremapMobs extends AddonJavaPlugin {
    private static SquaremapMobs instance;
    private SquaremapHook squaremapHook;
    private MobsConfig config;

    public SquaremapMobs() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.config = new MobsConfig(this);
        this.config.reload();

        @SuppressWarnings("unused") final String staticInit = Icons.class.getTypeName();

        this.squaremapHook = new SquaremapHook(this);
        this.squaremapHook.load();
    }

    @Override
    public void onDisable() {
        if (this.squaremapHook != null) {
            this.squaremapHook.disable();
        }
        this.squaremapHook = null;
        scheduler().getImpl().cancelAllTasks();
    }

    public static SquaremapMobs getInstance() {
        return instance;
    }

    public MobsConfig config() {
        return this.config;
    }
}

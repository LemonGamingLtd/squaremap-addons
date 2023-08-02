package xyz.jpenilla.squaremap.addon.claimchunk;

import xyz.jpenilla.squaremap.addon.claimchunk.config.ClaimChunkConfig;
import xyz.jpenilla.squaremap.addon.claimchunk.hook.SquaremapHook;
import xyz.jpenilla.squaremap.addon.common.AddonJavaPlugin;

public final class SquaremapClaimChunk extends AddonJavaPlugin {
    private SquaremapHook squaremapHook;
    private ClaimChunkConfig config;

    @Override
    public void onEnable() {
        this.config = new ClaimChunkConfig(this);
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

    public ClaimChunkConfig config() {
        return this.config;
    }
}

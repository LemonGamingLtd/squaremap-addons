package xyz.jpenilla.squaremap.addon.coreprotect;

import xyz.jpenilla.squaremap.addon.common.AddonJavaPlugin;
import xyz.jpenilla.squaremap.addon.coreprotect.hook.SquaremapChunkMarker;
import xyz.jpenilla.squaremap.addon.coreprotect.listener.CoreProtectRollbackListener;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

public final class SquaremapCoreProtect extends AddonJavaPlugin {

    @Override
    public void onEnable() {
        final SquaremapChunkMarker chunkMarker;
        try {
            chunkMarker = new SquaremapChunkMarker(SquaremapProvider.get());
        } catch (final ReflectiveOperationException ex) {
            this.getLogger().severe("This squaremap version is not supported by squaremap-coreprotect: " + ex);
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final CoreProtectRollbackListener listener;
        try {
            listener = new CoreProtectRollbackListener(this, chunkMarker);
        } catch (final ReflectiveOperationException ex) {
            this.getLogger().severe("The installed CoreProtect does not fire CoreProtectRollbackEvent. "
                + "squaremap-coreprotect needs the LemonGaming coreprotect-clickhouse build. (" + ex + ")");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        listener.register();
    }
}

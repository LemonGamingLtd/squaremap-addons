package xyz.jpenilla.squaremap.addon.vanish;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import xyz.jpenilla.squaremap.api.Squaremap;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VanishFallback implements VanishAdapter {

    public VanishFallback(final SquaremapVanish plugin, final Squaremap squaremap) {
        plugin.scheduler().getImpl().runTimer(() -> {
            for (final Player player : plugin.getServer().getOnlinePlayers()) {
                final boolean isVanished = isVanished(player);
                final UUID playerId = player.getUniqueId();
                if (isVanished != squaremap.playerManager().hidden(playerId)) {
                    squaremap.playerManager().hidden(playerId, isVanished);
                }
            }
        }, 1L, 1L, TimeUnit.SECONDS);
    }

    @Override
    public boolean isVanished(final Player player) {
        final List<MetadataValue> list = player.getMetadata("vanished");
        for (MetadataValue value : list) {
            if (value.asBoolean()) {
                return true;
            }
        }
        return false;
    }
}

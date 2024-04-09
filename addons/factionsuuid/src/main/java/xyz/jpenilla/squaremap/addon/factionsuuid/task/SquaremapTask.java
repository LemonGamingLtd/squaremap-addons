package xyz.jpenilla.squaremap.addon.factionsuuid.task;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import org.bukkit.World;
import xyz.jpenilla.squaremap.addon.common.scheduler.WrappedRunnable;
import xyz.jpenilla.squaremap.addon.factionsuuid.SquaremapFactions;
import xyz.jpenilla.squaremap.addon.factionsuuid.hook.FactionsHook;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.api.marker.Rectangle;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

public final class SquaremapTask extends WrappedRunnable {
    private final World bukkitWorld;
    private final SimpleLayerProvider provider;
    private final SquaremapFactions plugin;

    private boolean stop;

    public SquaremapTask(SquaremapFactions plugin, MapWorld world, SimpleLayerProvider provider) {
        this.plugin = plugin;
        this.bukkitWorld = BukkitAdapter.bukkitWorld(world);
        this.provider = provider;
    }

    @Override
    public void run() {
        if (this.stop) {
            this.cancel();
            return;
        }
        this.updateClaims();
    }

    void updateClaims() {
        this.provider.clearMarkers(); // TODO track markers instead of clearing them
        FactionsHook.getClaims().asMap().forEach(this::handleClaim);
    }

    private void handleClaim(Faction faction, Collection<FLocation> claims) {
        final String worldName = bukkitWorld.getName();

        final MarkerOptions.Builder options = MarkerOptions.builder()
            .strokeColor(faction.isWarZone() ? this.plugin.config().warzoneStrokeColor : this.plugin.config().strokeColor)
            .strokeWeight(this.plugin.config().strokeWeight)
            .strokeOpacity(this.plugin.config().strokeOpacity)
            .fillColor(faction.isWarZone() ? this.plugin.config().warzoneFillColor : this.plugin.config().fillColor)
            .fillOpacity(this.plugin.config().fillOpacity)
            .clickTooltip(
                this.plugin.config().claimTooltip
                    .replace("{world}", worldName)
                    .replace("{id}", faction.getId())
                    .replace("{tag}", faction.getTag())
                    .replace("{desc}", faction.getDescription())
                    .replace("{members}", faction.getFPlayers().stream().map(FPlayer::getName).collect(Collectors.joining(", ")))
                    .replace("{created}", Date.from(Instant.ofEpochMilli(faction.getFoundedDate())).toString())
            );

        for (final FLocation claim : claims) {
            if (!claim.getWorld().getUID().equals(bukkitWorld.getUID())) { // cheaper to filter here than upstream
                continue;
            }

            final long chunkX = claim.getX();
            final long chunkZ = claim.getZ();

            final long minX = (chunkX << 4);
            final long maxX = (chunkX << 4) | 15;

            final long minZ = (chunkZ << 4);
            final long maxZ = (chunkZ << 4) | 15;

            final Rectangle rectangle = Marker.rectangle(Point.of(minX, minZ), Point.of(maxX + 1, maxZ + 1));
            rectangle.markerOptions(options);

            final String markerId = "factions_claim_%s_%d_%d".formatted(faction.getId(), chunkX, chunkZ);
            this.provider.addMarker(Key.of(markerId), rectangle);
        }
    }

    public void disable() {
        this.cancel();
        this.stop = true;
        this.provider.clearMarkers();
    }
}


package xyz.jpenilla.squaremap.addon.factionsuuid.task;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import org.bukkit.World;
import xyz.jpenilla.squaremap.addon.common.scheduler.WrappedRunnable;
import xyz.jpenilla.squaremap.addon.factionsuuid.SquaremapFactions;
import xyz.jpenilla.squaremap.addon.factionsuuid.config.FactionsConfig;
import xyz.jpenilla.squaremap.addon.factionsuuid.hook.FactionsHook;
import xyz.jpenilla.squaremap.addon.factionsuuid.util.ChunkPolygonMerge;
import xyz.jpenilla.squaremap.addon.factionsuuid.util.ContiguousChunks;
import xyz.jpenilla.squaremap.addon.factionsuuid.web.FactionSearch;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.api.marker.Polygon;
import xyz.jpenilla.squaremap.api.marker.Rectangle;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class SquaremapTask extends WrappedRunnable {
    private final MapWorld world;
    private final World bukkitWorld;
    private final SimpleLayerProvider provider;
    private final SquaremapFactions plugin;

    private boolean stop;

    public SquaremapTask(SquaremapFactions plugin, MapWorld world, SimpleLayerProvider provider) {
        this.plugin = plugin;
        this.world = world;
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
        final List<FactionSearch.FactionClaims> searchData = new ArrayList<>();
        FactionsHook.getClaims(bukkitWorld).asMap().forEach((faction, claims) -> this.handleClaim(faction, claims, searchData));

        if (this.plugin.config().webSearch) {
            this.plugin.factionSearch().install();
            this.plugin.factionSearch().writeClaims(this.world, searchData);
        }
    }

    private void handleClaim(Faction faction, Collection<FLocation> claims, List<FactionSearch.FactionClaims> searchData) {
        final FactionsConfig config = this.plugin.config();
        final Color color = config.colorPerFaction && faction.isNormal() ? factionColor(faction.getId()) : null;
        final Map<FLocation, Long> raidClaims = FactionsHook.getRaidClaims(faction, claims);

        final List<FLocation> normalClaims = claims.stream().filter(claim -> !raidClaims.containsKey(claim)).toList();
        this.addMarkers(faction, normalClaims, color, null);
        if (!raidClaims.isEmpty()) {
            // a raid claim is placed in one go, so all of its chunks share an expiry; show the soonest
            final long expires = raidClaims.values().stream().mapToLong(Long::longValue).min().orElse(FactionsHook.UNKNOWN_EXPIRY);
            this.addMarkers(faction, raidClaims.keySet(), color, formatExpiry(expires));
        }

        if (faction.isNormal()) {
            searchData.add(new FactionSearch.FactionClaims(
                faction.getId(),
                faction.getTag(),
                hex(color != null ? color : config.fillColor),
                normalClaims.stream().map(claim -> new int[]{(int) claim.getX(), (int) claim.getZ()}).toList(),
                raidClaims.entrySet().stream().map(e -> new long[]{e.getKey().getX(), e.getKey().getZ(), e.getValue()}).toList()
            ));
        }
    }

    private void addMarkers(Faction faction, Collection<FLocation> claims, Color color, String raidExpiry) {
        if (claims.isEmpty()) {
            return;
        }
        final FactionsConfig config = this.plugin.config();
        final String worldName = bukkitWorld.getName();
        final boolean raid = raidExpiry != null;
        final String tag = escape(faction.getTag());

        final MarkerOptions.Builder options = MarkerOptions.builder()
            .strokeColor(faction.isWarZone() ? config.warzoneStrokeColor : raid ? config.raidClaimStrokeColor : color != null ? color : config.strokeColor)
            .strokeWeight(raid ? config.raidClaimStrokeWeight : config.strokeWeight)
            .strokeOpacity(config.strokeOpacity)
            .fillColor(faction.isWarZone() ? config.warzoneFillColor : color != null ? color : config.fillColor)
            .fillOpacity(config.fillOpacity)
            .hoverTooltip((raid ? config.raidClaimHoverTooltip : config.hoverTooltip)
                .replace("{tag}", tag)
                .replace("{expires}", raid ? raidExpiry : ""))
            .clickTooltip(
                (raid ? config.raidClaimTooltip.replace("{expires}", raidExpiry) : "") +
                config.claimTooltip
                    .replace("{world}", worldName)
                    .replace("{id}", faction.getId())
                    .replace("{tag}", tag)
                    .replace("{desc}", escape(faction.getDescription()))
                    .replace("{members}", faction.getFPlayers().stream().map(FPlayer::getName).map(SquaremapTask::escape).collect(Collectors.joining(", ")))
                    .replace("{created}", Date.from(Instant.ofEpochMilli(faction.getFoundedDate())).toString())
            );

        final String idPrefix = raid ? "factions_raidclaim_" : "factions_claim_";
        if (config.showChunks) {
            for (final FLocation claim : claims) {
                final long chunkX = claim.getX();
                final long chunkZ = claim.getZ();

                final long minX = (chunkX << 4);
                final long maxX = (chunkX << 4) | 15;

                final long minZ = (chunkZ << 4);
                final long maxZ = (chunkZ << 4) | 15;

                final Rectangle rectangle = Marker.rectangle(Point.of(minX, minZ), Point.of(maxX + 1, maxZ + 1));
                rectangle.markerOptions(options);

                final String markerId = idPrefix + "%s_%d_%d".formatted(faction.getId(), chunkX, chunkZ);
                this.provider.addMarker(Key.of(markerId), rectangle);
            }
            return;
        }

        final AtomicInteger groupIndex = new AtomicInteger();
        for (final List<FLocation> group : ContiguousChunks.group(claims)) {
            final List<int[]> coords = new ArrayList<>(group.size());
            for (final FLocation claim : group) {
                coords.add(new int[]{(int) claim.getX(), (int) claim.getZ()});
            }
            final Polygon polygon = ChunkPolygonMerge.toPolygon(coords);
            polygon.markerOptions(options);
            final String markerId = idPrefix + "%s_g%d".formatted(faction.getId(), groupIndex.getAndIncrement());
            this.provider.addMarker(Key.of(markerId), polygon);
        }
    }

    // golden-ratio hue steps keep consecutive faction ids far apart; reds are skipped since they mean warzone
    private static Color factionColor(String factionId) {
        final double step = (factionId.hashCode() * 0.6180339887498949) % 1.0;
        final float hue = (float) (0.06 + 0.88 * (step < 0 ? step + 1 : step));
        return Color.getHSBColor(hue, 0.75f, 0.95f);
    }

    private static String formatExpiry(long expiresAt) {
        if (expiresAt == FactionsHook.UNKNOWN_EXPIRY) {
            return "unknown";
        }
        final long minutes = Math.max(0, TimeUnit.MILLISECONDS.toMinutes(expiresAt - System.currentTimeMillis()));
        return minutes >= 60 ? "%dh %dm".formatted(minutes / 60, minutes % 60) : "%dm".formatted(minutes);
    }

    private static String hex(Color color) {
        return "#%06x".formatted(color.getRGB() & 0xFFFFFF);
    }

    // tags, descriptions and names are player-chosen and end up in the map page as HTML
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public void disable() {
        this.cancel();
        this.stop = true;
        this.provider.clearMarkers();
    }
}

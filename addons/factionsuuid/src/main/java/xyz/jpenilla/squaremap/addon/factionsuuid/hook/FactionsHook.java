package xyz.jpenilla.squaremap.addon.factionsuuid.hook;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.data.MemoryBoard;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class FactionsHook {
    public static final long UNKNOWN_EXPIRY = -1L;

    // raid claims only exist in LGFactions, so they're looked up reflectively and stock FactionsUUID just has none
    private static final Method GET_RAID_CLAIM_AT = findMethod(Faction.class, "getRaidClaimAt", FLocation.class);

    public static Multimap<Faction, FLocation> getClaims(@NotNull World world) {
        final List<Faction> factions = Factions.getInstance().getAllFactions();

        final int factionsAmount = factions.size();
        final int factionClaimsAmount = ((MemoryBoard) Board.getInstance()).flocationIds.size();

        final Multimap<Faction, FLocation> claims = HashMultimap.create(factionsAmount, (factionClaimsAmount / factionsAmount));
        for (final Faction faction : Factions.getInstance().getAllFactions()) {
            if (!faction.isNormal() && !faction.isWarZone()) {
                continue;
            }

            claims.putAll(
                faction,
                Board.getInstance().getAllClaims(faction).stream()
                    .filter(fLocation -> fLocation.getWorld().equals(world))
                    .collect(Collectors.toSet())
            );
        }

        return claims;
    }

    /**
     * Returns which of the given claims are raid claims, mapped to when each expires (epoch millis, or {@link #UNKNOWN_EXPIRY}).
     */
    public static Map<FLocation, Long> getRaidClaims(@NotNull Faction faction, @NotNull Collection<FLocation> claims) {
        if (GET_RAID_CLAIM_AT == null || !faction.isNormal()) {
            return Map.of();
        }

        final long expireMillis = raidClaimExpireMillis();
        final Map<FLocation, Long> raidClaims = new HashMap<>();
        try {
            for (final FLocation claim : claims) {
                // getRaidClaimAt skips expired claims and never touches the board
                final Object raidClaim = GET_RAID_CLAIM_AT.invoke(faction, claim);
                if (raidClaim == null) {
                    continue;
                }
                final long created = (long) raidClaim.getClass().getMethod("getCreationTime").invoke(raidClaim);
                raidClaims.put(claim, expireMillis == UNKNOWN_EXPIRY ? UNKNOWN_EXPIRY : created + expireMillis);
            }
        } catch (final ReflectiveOperationException | RuntimeException e) {
            // the main thread can change the raid claim list while we read it; the next update catches up
            return Map.of();
        }
        return raidClaims;
    }

    // FactionsPlugin.getInstance().conf().factions().raidClaims().raidClaimsExpireHours
    private static long raidClaimExpireMillis() {
        try {
            final Object plugin = Class.forName("com.massivecraft.factions.FactionsPlugin").getMethod("getInstance").invoke(null);
            final Object conf = plugin.getClass().getMethod("conf").invoke(plugin);
            final Object factions = conf.getClass().getMethod("factions").invoke(conf);
            final Object raidClaims = factions.getClass().getMethod("raidClaims").invoke(factions);
            return TimeUnit.HOURS.toMillis(raidClaims.getClass().getField("raidClaimsExpireHours").getInt(raidClaims));
        } catch (final ReflectiveOperationException | RuntimeException e) {
            return UNKNOWN_EXPIRY;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }
}

package xyz.jpenilla.squaremap.addon.factionsuuid.hook;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.data.MemoryBoard;

import java.util.List;

public final class FactionsHook {

    public static Multimap<Faction, FLocation> getClaims() {
        final List<Faction> factions = Factions.getInstance().getAllFactions();

        final int factionsAmount = factions.size();
        final int factionClaimsAmount = ((MemoryBoard) Board.getInstance()).flocationIds.size();

        final Multimap<Faction, FLocation> claims = HashMultimap.create(factionsAmount, (factionClaimsAmount / factionsAmount));
        for (final Faction faction : Factions.getInstance().getAllFactions()) {
            if (!faction.isNormal()) {
                continue;
            }
            claims.putAll(faction, Board.getInstance().getAllClaims(faction));
        }

        return claims;
    }
}

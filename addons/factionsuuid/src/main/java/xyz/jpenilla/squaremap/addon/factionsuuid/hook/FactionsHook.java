package xyz.jpenilla.squaremap.addon.factionsuuid.hook;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;

public final class FactionsHook {

    public static Multimap<Faction, FLocation> getClaims() {
        final Multimap<Faction, FLocation> claims = HashMultimap.create();
        for (final Faction faction : Factions.getInstance().getAllFactions()) {
            claims.putAll(faction, faction.getAllClaims());
        }
        return claims;
    }
}

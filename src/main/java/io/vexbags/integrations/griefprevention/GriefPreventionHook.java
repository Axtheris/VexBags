package io.vexbags.integrations.griefprevention;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class GriefPreventionHook {
    private final com.axther.vexBags.VexBags plugin;
    public GriefPreventionHook(com.axther.vexBags.VexBags plugin) { this.plugin = plugin; }

    public boolean hasContainerTrust(Player player, Location loc, boolean require) {
        if (!require) return true;
        try {
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object gp = gpClass.getMethod("instance").invoke(null);
            Object dataStore = gpClass.getField("dataStore").get(gp);
            // Claim getClaimAt(Location, boolean, double, boolean)
            Class<?> dataStoreClass = dataStore.getClass();
            Object claim = dataStoreClass.getMethod("getClaimAt", Location.class, boolean.class, double.class, boolean.class)
                    .invoke(dataStore, loc, true, 1.0, true);
            if (claim == null) return true; // not in claim
            // String allowContainers(Player)
            String res = (String) claim.getClass().getMethod("allowContainers", Player.class).invoke(claim, player);
            return res == null;
        } catch (Throwable t) {
            return true;
        }
    }
}



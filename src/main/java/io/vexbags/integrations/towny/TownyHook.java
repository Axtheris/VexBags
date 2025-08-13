package io.vexbags.integrations.towny;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class TownyHook {
    private final com.axther.vexBags.VexBags plugin;
    public TownyHook(com.axther.vexBags.VexBags plugin) { this.plugin = plugin; }

    public boolean hasRequiredTrust(Player player, Location loc, boolean requireContainerTrust) {
        try {
            // Use PlayerCacheUtil to check permissions at location
            Class<?> actionType = Class.forName("com.palmergames.bukkit.towny.object.ActionType");
            Class<?> pcUtil = Class.forName("com.palmergames.bukkit.towny.utils.PlayerCacheUtil");
            Object at = java.util.Arrays.stream(actionType.getEnumConstants())
                    .filter(e -> e.toString().equals("SWITCH")).findFirst().orElse(null);
            boolean canUse = (boolean) pcUtil.getMethod("getCachePermission", Player.class, Location.class, actionType)
                    .invoke(null, player, loc, at);
            if (!requireContainerTrust) return canUse;
            Object at2 = java.util.Arrays.stream(actionType.getEnumConstants())
                    .filter(e -> e.toString().equals("ITEM_USE")).findFirst().orElse(null);
            boolean canContainers = (boolean) pcUtil.getMethod("getCachePermission", Player.class, Location.class, actionType)
                    .invoke(null, player, loc, at2);
            return canUse && canContainers;
        } catch (Throwable t) {
            return true; // fail open
        }
    }
}



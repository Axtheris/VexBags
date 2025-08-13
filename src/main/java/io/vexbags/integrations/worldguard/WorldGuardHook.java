package io.vexbags.integrations.worldguard;

import com.axther.vexBags.VexBags;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WorldGuardHook {
    private final VexBags plugin;
    private Object VEXBAGS_ALLOW; // StateFlag

    public WorldGuardHook(VexBags plugin) { this.plugin = plugin; }

    public void registerFlag() {
        try {
            String def = plugin.getConfig().getString("integrations.worldguard.flag_default", "ALLOW");
            boolean allowDefault = !"DENY".equalsIgnoreCase(def);
            Class<?> stateFlag = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            this.VEXBAGS_ALLOW = stateFlag.getConstructor(String.class, boolean.class)
                    .newInstance("VEXBAGS_ALLOW", allowDefault);
            Object registry = Class.forName("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null);
            Object flagRegistry = registry.getClass().getMethod("getFlagRegistry").invoke(registry);
            try { flagRegistry.getClass().getMethod("register", stateFlag).invoke(flagRegistry, VEXBAGS_ALLOW); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public boolean isAllowedAt(Player player, Location location) {
        try {
            Object wg = Class.forName("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null);
            Object container = wg.getClass().getMethod("getPlatform").invoke(wg)
                    .getClass().getMethod("getRegionContainer").invoke(wg.getClass().getMethod("getPlatform").invoke(wg));
            Object query = container.getClass().getMethod("createQuery").invoke(container);
            Object adaptedLoc = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter").getMethod("adapt", Location.class)
                    .invoke(null, location);
            Object set = query.getClass().getMethod("getApplicableRegions", adaptedLoc.getClass()).invoke(query, adaptedLoc);
            if (set == null || VEXBAGS_ALLOW == null) return true;
            Object adaptedPlayer = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter").getMethod("adapt", org.bukkit.entity.Player.class)
                    .invoke(null, player);
            Object val = set.getClass().getMethod("queryState", adaptedPlayer.getClass(), VEXBAGS_ALLOW.getClass())
                    .invoke(set, adaptedPlayer, VEXBAGS_ALLOW);
            if (val == null) return true;
            return Boolean.TRUE.equals(val) || (val instanceof Enum<?> e && e.name().equals("ALLOW"));
        } catch (Throwable t) {
            return true;
        }
    }
}



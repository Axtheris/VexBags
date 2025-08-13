package io.vexbags.integrations.combatlogx;

import com.axther.vexBags.VexBags;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public final class CombatLogXHook implements Listener {
    private final VexBags plugin;
    private final boolean closeOnTag;
    private Object combatManager; // ICombatManager
    private org.bukkit.scheduler.BukkitTask pollTask;

    public CombatLogXHook(VexBags plugin, boolean closeOnTag) {
        this.plugin = plugin;
        this.closeOnTag = closeOnTag;
        try {
            Object pl = Bukkit.getPluginManager().getPlugin("CombatLogX");
            Class<?> apiClass = Class.forName("com.github.sirblobman.combatlogx.api.ICombatLogX");
            if (apiClass.isInstance(pl)) {
                this.combatManager = apiClass.getMethod("getCombatManager").invoke(pl);
            }
        } catch (Throwable ignored) {}
    }

    public void registerDynamicListeners() {
        if (!closeOnTag) return;
        // Poll every 5 ticks: if a player is tagged while viewing a backpack, close it.
        this.pollTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    if (!isTagged(p)) continue;
                    var top = p.getOpenInventory().getTopInventory();
                    if (top != null && top.getHolder() instanceof com.axther.vexBags.gui.BackpackHolder) {
                        p.closeInventory();
                        // Optional feedback message
                        try {
                            if (plugin.getConfig().getBoolean("messages.enabled", true)) {
                                String msg = plugin.getConfig().getString("messages.combatlogx.closed_on_tag", "<red>Backpack closed: you are in combat.</red>");
                                com.axther.vexBags.util.ItemUtil.sendPrefixed(p, com.axther.vexBags.util.ItemUtil.mm().deserialize(msg));
                            }
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
            }
        }, 20L, 5L);
    }

    public void unregisterDynamicListeners() {
        if (pollTask != null) { pollTask.cancel(); pollTask = null; }
    }

    public boolean isTagged(Player player) {
        try {
            if (combatManager == null) return false;
            return (boolean) combatManager.getClass().getMethod("isInCombat", Player.class).invoke(combatManager, player);
        } catch (Throwable t) {
            return false;
        }
    }
}



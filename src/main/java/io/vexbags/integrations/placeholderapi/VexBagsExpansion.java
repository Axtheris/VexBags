package io.vexbags.integrations.placeholderapi;

import com.axther.vexBags.VexBags;
import com.axther.vexBags.storage.BackpackData;
import com.axther.vexBags.storage.BackpackStorage;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VexBagsExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
    private final VexBags plugin;

    public VexBagsExpansion(VexBags plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "vexbags"; }
    @Override public String getAuthor() { return String.join(", ", plugin.getDescription().getAuthors()); }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null) return "";
        Player player = offlinePlayer.getPlayer();
        UUID pid = offlinePlayer.getUniqueId();
        if (pid == null) return "";

        if (params.equalsIgnoreCase("tier")) {
            if (player != null) {
                var hand = player.getInventory().getItemInMainHand();
                var tier = com.axther.vexBags.util.ItemUtil.getTier(hand);
                return tier == null ? "" : tier.name().toLowerCase();
            }
            return "";
        }

        if (params.equalsIgnoreCase("types_used")) {
            int total = 0;
            for (BackpackData d : BackpackStorage.get().all().values()) {
                if (pid.equals(d.getOwnerId())) total += d.totalItemTypes();
            }
            return Integer.toString(total);
        }

        if (params.equalsIgnoreCase("types_max")) {
            int max = 0;
            for (BackpackData d : BackpackStorage.get().all().values()) {
                if (!pid.equals(d.getOwnerId())) continue;
                max += d.getTier().getStorageSlots();
            }
            return Integer.toString(max);
        }

        if (params.toLowerCase().startsWith("item_total:")) {
            String material = params.substring("item_total:".length()).trim().toUpperCase();
            int count = 0;
            for (BackpackData d : BackpackStorage.get().all().values()) {
                if (!pid.equals(d.getOwnerId())) continue;
                for (var e : d.getEntries().entrySet()) {
                    var item = e.getValue().getTemplate();
                    if (item.getType().name().equalsIgnoreCase(material)) count += e.getValue().getAmount();
                }
            }
            return Integer.toString(count);
        }

        if (params.equalsIgnoreCase("owned_count")) {
            int count = 0;
            for (BackpackData d : BackpackStorage.get().all().values()) {
                if (pid.equals(d.getOwnerId())) count++;
            }
            return Integer.toString(count);
        }

        return "";
    }
}



package io.vexbags.integrations.itemsadder;

import com.axther.vexBags.VexBags;
import com.axther.vexBags.tier.BackpackTier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public final class ItemsAdderHook {
    private final VexBags plugin;
    public ItemsAdderHook(VexBags plugin) { this.plugin = plugin; }

    public ItemStack itemForTier(BackpackTier tier) {
        FileConfiguration cfg = plugin.getConfig();
        String root = "integrations.itemsadder.tiers." + tier.name().toLowerCase();
        String id = cfg.getString(root + ".item", "");
        if (id != null && !id.isEmpty()) {
            try {
                Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
                Object cs = customStack.getMethod("getInstance", String.class).invoke(null, id);
                if (cs != null) {
                    Object stack = customStack.getMethod("getItemStack").invoke(cs);
                    if (stack instanceof ItemStack it) {
                        ItemStack clone = it.clone();
                        clone.setAmount(1);
                        return clone;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }
}



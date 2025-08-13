package io.vexbags.integrations.oraxen;

import com.axther.vexBags.VexBags;
import com.axther.vexBags.tier.BackpackTier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public final class OraxenHook {
    private final VexBags plugin;
    public OraxenHook(VexBags plugin) { this.plugin = plugin; }

    public ItemStack itemForTier(BackpackTier tier) {
        FileConfiguration cfg = plugin.getConfig();
        String root = "integrations.oraxen.tiers." + tier.name().toLowerCase();
        String id = cfg.getString(root + ".item", "");
        if (id != null && !id.isEmpty()) {
            try {
                Class<?> oraxenItems = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                Object oxItem = oraxenItems.getMethod("getItemById", String.class).invoke(null, id);
                if (oxItem != null) {
                    // Prefer build() if available, else toItem()
                    ItemStack built = null;
                    try {
                        Object tmp = oxItem.getClass().getMethod("build").invoke(oxItem);
                        if (tmp instanceof ItemStack it) built = it;
                    } catch (Throwable ignore) {}
                    if (built == null) {
                        Object tmp = oxItem.getClass().getMethod("build").invoke(oxItem);
                        if (tmp instanceof ItemStack it) built = it;
                    }
                    if (built != null) {
                        ItemStack stack = built.clone();
                        stack.setAmount(1);
                        return stack;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }
}



package com.axther.vexBags.gui;

import com.axther.vexBags.storage.BackpackData;
import com.axther.vexBags.storage.BackpackStorage;
import com.axther.vexBags.tier.BackpackTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BackpackGui {
	private final UUID backpackId;
	private final BackpackTier tier;
	private Inventory inventory;

	public BackpackGui(UUID backpackId, BackpackTier tier) {
		this.backpackId = backpackId;
		this.tier = tier;
	}

	public void open(Player player) {
		int size = Math.max(9, Math.min(54, ((tier.getStorageSlots() + 8) / 9) * 9));
		BackpackHolder holder = new BackpackHolder(backpackId, tier);
        this.inventory = Bukkit.createInventory(holder, size, com.axther.vexBags.util.ItemUtil.mm().deserialize("<color:#" + com.axther.vexBags.util.ItemUtil.normalizeHex(tier.getHexColor()) + ">" + com.axther.vexBags.util.ItemUtil.toSmallCaps(tier.getDisplayName() + " backpack") + "</color>").decoration(TextDecoration.ITALIC, false));
		holder.setInventory(this.inventory);
		refresh();
		player.openInventory(inventory);
	}

	public void refresh() {
		if (inventory == null) return;
		inventory.clear();
		BackpackData data = BackpackStorage.get().get(backpackId);
		if (data == null) return;

		int slot = 0;
		for (Map.Entry<Material, Integer> e : data.getItemCounts().entrySet()) {
			if (slot >= tier.getStorageSlots()) break;
			ItemStack display = new ItemStack(e.getKey());
			ItemMeta meta = display.getItemMeta();
            List<Component> lore = new ArrayList<>();
            lore.add(com.axther.vexBags.util.ItemUtil.mm().deserialize("<white>total: " + e.getValue() + "</white>").decoration(TextDecoration.ITALIC, false));
            lore.add(com.axther.vexBags.util.ItemUtil.CLICK_HINT);
			meta.lore(lore);
			display.setItemMeta(meta);
			display.setAmount(Math.min(e.getValue(), display.getMaxStackSize()));
			inventory.setItem(slot++, display);
		}
	}

	public UUID getBackpackId() { return backpackId; }
	public BackpackTier getTier() { return tier; }
	public Inventory getInventory() { return inventory; }
}


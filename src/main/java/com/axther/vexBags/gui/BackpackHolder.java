package com.axther.vexBags.gui;

import com.axther.vexBags.tier.BackpackTier;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class BackpackHolder implements InventoryHolder {
	private final UUID backpackId;
	private final BackpackTier tier;
	private Inventory inventory;

	public BackpackHolder(UUID backpackId, BackpackTier tier) {
		this.backpackId = backpackId;
		this.tier = tier;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}

	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}

	public UUID getBackpackId() { return backpackId; }
	public BackpackTier getTier() { return tier; }
}


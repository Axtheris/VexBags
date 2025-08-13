package com.axther.vexBags.tier;

import org.bukkit.Material;

public enum BackpackTier {
	LEATHER("Leather", "#a87c5a", Material.LEATHER, 9, 1),
	COPPER("Copper", "#b87333", Material.COPPER_INGOT, 18, 2),
	IRON("Iron", "#c7c7c7", Material.IRON_INGOT, 27, 3),
	GOLD("Gold", "#ffd700", Material.GOLD_INGOT, 36, 4),
	DIAMOND("Diamond", "#3de2ff", Material.DIAMOND, 45, 5),
	NETHERITE("Netherite", "#6b3fa0", Material.NETHERITE_INGOT, 54, 6);

	private final String displayName;
	private final String hexColor;
	private final Material upgradeMaterial;
	private final int storageSlots;
	private final int upgradeSlots;

	BackpackTier(String displayName, String hexColor, Material upgradeMaterial, int storageSlots, int upgradeSlots) {
		this.displayName = displayName;
		this.hexColor = hexColor;
		this.upgradeMaterial = upgradeMaterial;
		this.storageSlots = storageSlots;
		this.upgradeSlots = upgradeSlots;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getHexColor() {
		return hexColor;
	}

	public Material getUpgradeMaterial() {
		return upgradeMaterial;
	}

	public int getStorageSlots() {
		return storageSlots;
	}

	public int getUpgradeSlots() {
		return upgradeSlots;
	}

	public BackpackTier next() {
		int ordinal = this.ordinal();
		if (ordinal + 1 < values().length) {
			return values()[ordinal + 1];
		}
		return this;
	}

	public static BackpackTier fromString(String name) {
		for (BackpackTier value : values()) {
			if (value.name().equalsIgnoreCase(name) || value.displayName.equalsIgnoreCase(name)) {
				return value;
			}
		}
		return null;
	}
}


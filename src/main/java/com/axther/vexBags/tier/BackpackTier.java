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
		try {
			org.bukkit.configuration.file.FileConfiguration cfg = com.axther.vexBags.VexBags.getInstance().getConfig();
			String path = "tiers." + name().toLowerCase() + ".display_name";
			String v = cfg.getString(path, displayName);
			return (v == null || v.isEmpty()) ? displayName : v;
		} catch (Exception ignore) {
			return displayName;
		}
	}

	public String getHexColor() {
		try {
			org.bukkit.configuration.file.FileConfiguration cfg = com.axther.vexBags.VexBags.getInstance().getConfig();
			String path = "tiers." + name().toLowerCase() + ".hex_color";
			String v = cfg.getString(path, hexColor);
			return (v == null || v.isEmpty()) ? hexColor : v;
		} catch (Exception ignore) {
			return hexColor;
		}
	}

	public Material getUpgradeMaterial() {
		return upgradeMaterial;
	}


	public int getStorageSlots() {
		// Allow override from config
		try {
			org.bukkit.configuration.file.FileConfiguration cfg = com.axther.vexBags.VexBags.getInstance().getConfig();
			String path = "tiers." + name().toLowerCase() + ".slots";
			int v = cfg.getInt(path, storageSlots);
			return v > 0 ? v : storageSlots;
		} catch (Exception ignore) {
			return storageSlots;
		}
	}

	public int getUpgradeSlots() {
		return upgradeSlots;
	}

	public org.bukkit.Material getBackpackMaterial() {
		try {
			org.bukkit.configuration.file.FileConfiguration cfg = com.axther.vexBags.VexBags.getInstance().getConfig();
			String p = "tiers." + name().toLowerCase() + ".material";
			String mat = cfg.getString(p, "SHULKER_BOX");
			org.bukkit.Material m = org.bukkit.Material.matchMaterial(mat);
			return m != null ? m : org.bukkit.Material.SHULKER_BOX;
		} catch (Exception ignore) {
			return org.bukkit.Material.SHULKER_BOX;
		}
	}

	public int getCustomModelData() {
		try {
			org.bukkit.configuration.file.FileConfiguration cfg = com.axther.vexBags.VexBags.getInstance().getConfig();
			String p = "tiers." + name().toLowerCase() + ".custom_model_data";
			return cfg.getInt(p, 0);
		} catch (Exception ignore) {
			return 0;
		}
	}

	public int getPerSlotMax() {
		try {
			org.bukkit.configuration.file.FileConfiguration cfg = com.axther.vexBags.VexBags.getInstance().getConfig();
			String path = "tiers." + name().toLowerCase() + ".per_slot_max";
			int def = 64;
			int v = cfg.getInt(path, def);
			return Math.max(1, v);
		} catch (Exception ignore) {
			return 64;
		}
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


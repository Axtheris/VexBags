package com.axther.vexBags.tier;

import com.axther.vexBags.VexBags;
import org.bukkit.Material;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class TierRegistry {
	private static final Map<String, TierSpec> idToSpec = new LinkedHashMap<>();

	public static void load(VexBags plugin) {
		idToSpec.clear();
		// Seed with enum defaults
		for (BackpackTier t : BackpackTier.values()) {
			idToSpec.put(t.name().toLowerCase(), TierSpec.fromEnum(t));
		}
		File file = new File(plugin.getDataFolder(), "tiers.json");
		if (!file.exists()) {
			save(plugin);
			return;
		}
		try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
			var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
			TierSpec[] arr = gson.fromJson(reader, TierSpec[].class);
			if (arr != null) {
				for (TierSpec spec : arr) {
					if (spec == null || spec.id == null) continue;
					idToSpec.put(spec.id.toLowerCase(), spec);
				}
			}
		} catch (Exception ignored) {}
	}

	public static void save(VexBags plugin) {
		try {
			File file = new File(plugin.getDataFolder(), "tiers.json");
			file.getParentFile().mkdirs();
			var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
			TierSpec[] arr = idToSpec.values().toArray(new TierSpec[0]);
			try (FileWriter w = new FileWriter(file, StandardCharsets.UTF_8)) {
				gson.toJson(arr, w);
			}
		} catch (Exception ignored) {}
	}

	public static TierSpec get(String idOrEnum) {
		if (idOrEnum == null) return null;
		TierSpec spec = idToSpec.get(idOrEnum.toLowerCase());
		if (spec != null) return spec;
		try {
			BackpackTier t = BackpackTier.valueOf(idOrEnum.toUpperCase());
			return idToSpec.get(t.name().toLowerCase());
		} catch (Exception e) {
			return null;
		}
	}

	public static Map<String, TierSpec> all() { return java.util.Collections.unmodifiableMap(idToSpec); }

	public record TierSpec(String id, String displayName, String hexColor, String material,
						int slots, int perSlotMax, int upgradeSlots, int customModelData) {
		public Material materialEnum() { return Material.matchMaterial(material) == null ? Material.SHULKER_BOX : Material.matchMaterial(material); }
		public static TierSpec fromEnum(BackpackTier t) {
			return new TierSpec(
				t.name().toLowerCase(),
				t.getDisplayName(),
				t.getHexColor(),
				Material.SHULKER_BOX.name(),
				t.getStorageSlots(),
				64,
				t.getUpgradeSlots(),
				0
			);
		}
	}
}



package com.axther.vexBags.storage;

import com.axther.vexBags.VexBags;
import com.axther.vexBags.tier.BackpackTier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackpackStorage {
	private static final BackpackStorage INSTANCE = new BackpackStorage();
	public static BackpackStorage get() { return INSTANCE; }

	private final Map<UUID, BackpackData> idToData = new HashMap<>();
    private File dataFile;
    private FileConfiguration config;
    private File indexFile;
    // Reserved for future use (external tools may read it from disk directly)
    // private FileConfiguration indexConfig;
    private VexBags plugin;
    private BukkitTask pendingSaveTask;

    public void init(VexBags plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.dataFile = new File(plugin.getDataFolder(), "backpacks.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        this.indexFile = new File(plugin.getDataFolder(), "index.yml");
        // this.indexConfig = YamlConfiguration.loadConfiguration(indexFile);
        loadAll();
    }

	public synchronized BackpackData getOrCreate(UUID id, BackpackTier tier) {
		BackpackData data = idToData.get(id);
		if (data == null) {
			data = new BackpackData(id, tier);
			idToData.put(id, data);
		}
		return data;
	}

	public synchronized BackpackData get(UUID id) { return idToData.get(id); }

    public synchronized void saveNow() {
        FileConfiguration snapshot = new YamlConfiguration();
        ConfigurationSection root = snapshot.createSection("backpacks");
        for (BackpackData data : idToData.values()) {
            ConfigurationSection sec = root.createSection(data.getId().toString());
            sec.set("tier", data.getTier().name());
            if (data.getOwnerId() != null) sec.set("owner", data.getOwnerId().toString());
            sec.set("created_at", data.getCreatedAtEpochMs());
            sec.set("updated_at", data.getUpdatedAtEpochMs());
            ConfigurationSection items = sec.createSection("items");
            Map<Material, Integer> counts = data.snapshotItemCounts();
            for (Map.Entry<Material, Integer> e : counts.entrySet()) {
                items.set(e.getKey().name(), e.getValue());
            }
        }
        try {
            snapshot.save(dataFile);
            this.config = snapshot;
            // Build and save owner index
            FileConfiguration idx = new YamlConfiguration();
            ConfigurationSection owners = idx.createSection("owners");
            for (BackpackData data : idToData.values()) {
                if (data.getOwnerId() == null) continue;
                String key = data.getOwnerId().toString();
                ConfigurationSection list = owners.getConfigurationSection(key);
                if (list == null) list = owners.createSection(key);
                list.set(data.getId().toString(), data.getTier().name());
            }
            idx.save(indexFile);
            // this.indexConfig = idx;
        } catch (IOException e) {
            Bukkit.getLogger().severe("[VexBags] Failed to save backpacks.yml: " + e.getMessage());
        }
    }

    public synchronized void scheduleSave() {
        if (plugin == null) return;
        if (pendingSaveTask != null) {
            pendingSaveTask.cancel();
        }
        // Debounce: save ~2s after the last request
        pendingSaveTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::saveNow, 40L);
    }

	private synchronized void loadAll() {
		idToData.clear();
        ConfigurationSection root = config.getConfigurationSection("backpacks");
		if (root == null) return;
		for (String idStr : root.getKeys(false)) {
			UUID id;
			try { id = UUID.fromString(idStr); } catch (IllegalArgumentException ex) { continue; }
			ConfigurationSection sec = root.getConfigurationSection(idStr);
			if (sec == null) continue;
			BackpackTier tier = BackpackTier.fromString(sec.getString("tier", "LEATHER"));
			if (tier == null) tier = BackpackTier.LEATHER;
			BackpackData data = new BackpackData(id, tier);
            String owner = sec.getString("owner", null);
            if (owner != null) {
                try { data.setOwnerId(UUID.fromString(owner)); } catch (IllegalArgumentException ignored) {}
            }
            data.setTier(tier);
            // Timestamps
            data.touch();
			ConfigurationSection items = sec.getConfigurationSection("items");
			if (items != null) {
				for (String key : items.getKeys(false)) {
					Material mat = Material.matchMaterial(key);
					if (mat != null) {
						int amount = items.getInt(key, 0);
						if (amount > 0) data.add(mat, amount);
					}
				}
			}
			idToData.put(id, data);
		}
	}

    public synchronized void setOwner(UUID backpackId, UUID ownerId) {
        BackpackData data = idToData.get(backpackId);
        if (data == null) return;
        data.setOwnerId(ownerId);
        scheduleSave();
    }

    public synchronized Map<UUID, BackpackData> all() {
        return new HashMap<>(idToData);
    }
}


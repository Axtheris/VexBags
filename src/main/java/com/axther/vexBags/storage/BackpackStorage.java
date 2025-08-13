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
    private File jsonDataFile;
    private File jsonIndexFile;
    private VexBags plugin;
    private BukkitTask pendingSaveTask;

    public void init(VexBags plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.jsonDataFile = new File(plugin.getDataFolder(), "backpacks.json");
        this.jsonIndexFile = new File(plugin.getDataFolder(), "index.json");
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
        if (jsonDataFile == null) return;
        java.util.List<java.util.Map<String,Object>> jsonList = new java.util.ArrayList<>();
        for (BackpackData data : idToData.values()) {
            java.util.Map<String,Object> jo = new java.util.LinkedHashMap<>();
            jo.put("id", data.getId().toString());
            jo.put("tier", data.getTier().name());
            if (data.getOwnerId() != null) jo.put("owner", data.getOwnerId().toString());
            jo.put("created_at", data.getCreatedAtEpochMs());
            jo.put("updated_at", data.getUpdatedAtEpochMs());
            jo.put("items", data.snapshotCounts());
            jsonList.add(jo);
        }
        try {
            var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            try (java.io.FileWriter fw = new java.io.FileWriter(jsonDataFile, java.nio.charset.StandardCharsets.UTF_8)) {
                gson.toJson(jsonList, fw);
            }
            java.util.Map<String, java.util.Map<String,String>> ownersJson = new java.util.LinkedHashMap<>();
            for (BackpackData data : idToData.values()) {
                if (data.getOwnerId() == null) continue;
                String key = data.getOwnerId().toString();
                ownersJson.computeIfAbsent(key, k -> new java.util.LinkedHashMap<>())
                        .put(data.getId().toString(), data.getTier().name());
            }
            try (java.io.FileWriter fw = new java.io.FileWriter(jsonIndexFile, java.nio.charset.StandardCharsets.UTF_8)) {
                gson.toJson(ownersJson, fw);
            }
        } catch (java.io.IOException e) {
            Bukkit.getLogger().severe("[VexBags] Failed to save backpack json: " + e.getMessage());
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
        // Prefer JSON if present
        if (jsonDataFile.exists()) {
            try (java.io.FileReader fr = new java.io.FileReader(jsonDataFile, java.nio.charset.StandardCharsets.UTF_8)) {
                var gson = new com.google.gson.Gson();
                com.google.gson.JsonArray arr = gson.fromJson(fr, com.google.gson.JsonArray.class);
                if (arr != null) {
                    for (var el : arr) {
                        if (!el.isJsonObject()) continue;
                        var o = el.getAsJsonObject();
                        String idStr = o.get("id").getAsString();
                        UUID id;
                        try { id = UUID.fromString(idStr); } catch (Exception ex) { continue; }
                        String tierName = o.get("tier").getAsString();
                        BackpackTier tier = BackpackTier.fromString(tierName);
                        if (tier == null) tier = BackpackTier.LEATHER;
                        BackpackData data = new BackpackData(id, tier);
                        if (o.has("owner")) { try { data.setOwnerId(UUID.fromString(o.get("owner").getAsString())); } catch (Exception ignore) {} }
                        if (o.has("items")) {
                            var items = o.getAsJsonObject("items");
                            for (var e : items.entrySet()) {
                                String key = e.getKey();
                                int amount = e.getValue().getAsInt();
                                Material mat = Material.matchMaterial(key);
                                org.bukkit.inventory.ItemStack proxy = new org.bukkit.inventory.ItemStack(mat == null ? Material.STONE : mat, 1);
                                data.add(proxy, com.axther.vexBags.util.ItemUtil.stackKey(proxy), amount);
                            }
                        }
                        idToData.put(id, data);
                    }
                }
            } catch (Exception ex) {
                Bukkit.getLogger().severe("[VexBags] Failed to load backpack json: " + ex.getMessage());
            }
            return;
        }
        // No YAML fallback: initialize empty JSON file if missing
        saveNow();
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

    public synchronized boolean deleteBackpack(UUID id) {
        BackpackData removed = idToData.remove(id);
        if (removed != null) {
            scheduleSave();
            return true;
        }
        return false;
    }
}


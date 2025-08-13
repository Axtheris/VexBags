package com.axther.vexBags.storage;

import com.axther.vexBags.tier.BackpackTier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class BackpackData {
	private final UUID id;
	private BackpackTier tier;
    private final Map<String, StoredItem> keyToItem;
    private UUID ownerId; // nullable until assigned
    private long createdAtEpochMs;
    private long updatedAtEpochMs;

    public BackpackData(UUID id, BackpackTier tier) {
        this.id = id;
        this.tier = tier;
        this.keyToItem = new LinkedHashMap<>();
        this.createdAtEpochMs = System.currentTimeMillis();
        this.updatedAtEpochMs = this.createdAtEpochMs;
    }

	public UUID getId() { return id; }

	public BackpackTier getTier() { return tier; }

	public void setTier(BackpackTier tier) { this.tier = tier; }

    public Map<String, StoredItem> getEntries() { return Collections.unmodifiableMap(keyToItem); }

    public Map<String, Integer> snapshotCounts() {
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, StoredItem> e : keyToItem.entrySet()) out.put(e.getKey(), e.getValue().getAmount());
        return out;
    }

    public int getCountByKey(String key) { return keyToItem.get(key) == null ? 0 : keyToItem.get(key).getAmount(); }

    public void add(ItemStack stack, String key, int amount) {
        if (amount <= 0) return;
        StoredItem existing = keyToItem.get(key);
        if (existing == null) {
            existing = new StoredItem(key, stack, amount);
            keyToItem.put(key, existing);
        } else {
            existing.add(amount);
        }
        this.updatedAtEpochMs = System.currentTimeMillis();
    }

    public int removeByKey(String key, int amount) {
		if (amount <= 0) return 0;
        StoredItem st = keyToItem.get(key);
        int have = st == null ? 0 : st.getAmount();
		int removed = Math.min(have, amount);
		if (removed <= 0) return 0;
        int left = have - removed;
        if (left <= 0) keyToItem.remove(key); else st.remove(removed);
        this.updatedAtEpochMs = System.currentTimeMillis();
		return removed;
	}

    public int totalItemTypes() { return keyToItem.size(); }

    public int totalItems() { return keyToItem.values().stream().mapToInt(StoredItem::getAmount).sum(); }

    public UUID getOwnerId() { return ownerId; }

    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public long getCreatedAtEpochMs() { return createdAtEpochMs; }

    public long getUpdatedAtEpochMs() { return updatedAtEpochMs; }

    public void touch() { this.updatedAtEpochMs = System.currentTimeMillis(); }
}


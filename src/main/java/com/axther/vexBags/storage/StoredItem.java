package com.axther.vexBags.storage;

import org.bukkit.inventory.ItemStack;

public class StoredItem {
	private final String key;
	private final ItemStack template;
	private int amount;

	public StoredItem(String key, ItemStack template, int amount) {
		this.key = key;
		this.template = template.clone();
		this.template.setAmount(1);
		this.amount = amount;
	}

	public String getKey() { return key; }
	public ItemStack getTemplate() { return template.clone(); }
	public int getAmount() { return amount; }

	public void add(int add) { if (add > 0) this.amount += add; }

	public int remove(int remove) {
		if (remove <= 0) return 0;
		int r = Math.min(remove, amount);
		amount -= r;
		return r;
	}
}


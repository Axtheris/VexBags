package com.axther.vexBags.listener;

import com.axther.vexBags.gui.BackpackGui;
import com.axther.vexBags.storage.BackpackData;
import com.axther.vexBags.storage.BackpackStorage;
import com.axther.vexBags.tier.BackpackTier;
import com.axther.vexBags.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class BackpackListeners implements Listener {

	@EventHandler
	public void onCraft(CraftItemEvent event) {
		ItemStack result = event.getCurrentItem();
		if (result == null) return;
		if (!ItemUtil.isBackpack(result)) return;
		// Assign or preserve ID and create storage
		UUID id = ItemUtil.getBackpackId(result);
		BackpackTier tier = ItemUtil.getTier(result);
		if (tier == null) return;
		// Try preserve ID from any input backpack (upgrade path)
		for (ItemStack matrixItem : event.getInventory().getMatrix()) {
			if (matrixItem == null) continue;
			if (ItemUtil.isBackpack(matrixItem)) {
				UUID mId = ItemUtil.getBackpackId(matrixItem);
				if (mId != null) { id = mId; break; }
			}
		}
		if (id == null) {
			id = UUID.randomUUID();
		}
		ItemUtil.setBackpackId(result, id);
		BackpackData data = BackpackStorage.get().getOrCreate(id, tier);
		data.setTier(tier);
		ItemUtil.updateBackpackLore(result, data);
		ItemUtil.syncShulkerPreview(result, data);
		BackpackStorage.get().scheduleSave();
	}

	@EventHandler
	public void onPrepareCraft(PrepareItemCraftEvent event) {
		ItemStack result = event.getInventory().getResult();
		if (result == null) return;
		if (!ItemUtil.isBackpack(result)) return;
		BackpackTier tier = ItemUtil.getTier(result);
		if (tier == null) return;
		if (tier == BackpackTier.LEATHER) return; // base crafting fine
		// Ensure input contains a previous-tier backpack (our item), otherwise invalidate
		BackpackTier prev = BackpackTier.values()[tier.ordinal() - 1];
		boolean hasPrev = false;
		for (ItemStack matrixItem : event.getInventory().getMatrix()) {
			if (matrixItem == null) continue;
			if (ItemUtil.isBackpack(matrixItem) && ItemUtil.getTier(matrixItem) == prev) {
				hasPrev = true;
				break;
			}
		}
		if (!hasPrev) {
			event.getInventory().setResult(new ItemStack(Material.AIR));
		}
	}

	@EventHandler
	public void onUse(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;
		ItemStack item = event.getItem();
		if (!ItemUtil.isBackpack(item)) return;
		// Verify signature before allowing interaction
		if (!ItemUtil.verifyBackpackSignature(item)) {
			event.setCancelled(true);
			((Player) event.getPlayer()).sendMessage(net.kyori.adventure.text.Component.text("This backpack failed verification.").color(net.kyori.adventure.text.format.NamedTextColor.RED));
			return;
		}
		event.setCancelled(true);
		UUID id = ItemUtil.getBackpackId(item);
		BackpackTier tier = ItemUtil.getTier(item);
		if (id == null || tier == null) return;
		BackpackGui gui = new BackpackGui(id, tier);
		gui.open(event.getPlayer());
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		ItemStack item = event.getItemInHand();
		if (!ItemUtil.isBackpack(item)) return;
		// Prevent any placement of backpack shulker boxes
		event.setCancelled(true);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) return;
		if (event.getClickedInventory() == null) return;
		var holder = event.getView().getTopInventory().getHolder();
		if (!(holder instanceof com.axther.vexBags.gui.BackpackHolder bpHolder)) return;
		// Prevent normal item moving
		event.setCancelled(true);

		UUID id = bpHolder.getBackpackId();
		BackpackData data = BackpackStorage.get().get(id);
		if (data == null) return;

		boolean clickedTop = event.getClickedInventory() == event.getView().getTopInventory();

		if (clickedTop) {
			ItemStack clicked = event.getCurrentItem();
			if (clicked == null || clicked.getType() == Material.AIR) return;
			Material material = clicked.getType();
			int available = data.getCount(material);
			if (available <= 0) return;

			int desired = 0;
			switch (event.getClick()) {
				case LEFT -> desired = Math.min(1, available);
				case RIGHT -> desired = Math.min(8, available);
				case SHIFT_LEFT -> desired = Math.min(16, available);
				case SHIFT_RIGHT -> desired = Math.min(32, available);
				case MIDDLE -> desired = Math.min(64, available);
				case SWAP_OFFHAND -> {
					if (event.isShiftClick()) {
						desired = available; // Shift+F withdraw all available
					} else {
						int capacity = computeCapacityFor(player, material);
						desired = Math.min(available, Math.max(0, capacity)); // F fill all available space
					}
				}
				default -> desired = 0;
			}

			if (desired <= 0) return;
			int removed = data.remove(material, desired);
			if (removed > 0) {
				if (event.getClick() == ClickType.SWAP_OFFHAND) {
					// Put as much as possible in offhand, remainder to inventory
					ItemStack off = player.getInventory().getItemInOffHand();
					int max = Math.max(1, material.getMaxStackSize());
					if (off == null || off.getType() == Material.AIR) {
						int put = Math.min(removed, max);
						player.getInventory().setItemInOffHand(new ItemStack(material, put));
						int leftover = removed - put;
						if (leftover > 0) giveToPlayer(player, material, leftover);
					} else if (off.getType() == material) {
						int space = Math.max(0, max - off.getAmount());
						int put = Math.min(space, removed);
						off.setAmount(off.getAmount() + put);
						int leftover = removed - put;
						if (leftover > 0) giveToPlayer(player, material, leftover);
					} else {
						// Offhand busy with another item; give to inventory
						giveToPlayer(player, material, removed);
					}
				} else {
					giveToPlayer(player, material, removed);
				}
				BackpackStorage.get().scheduleSave();
			}

			// Update backpack item in hand if present
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (ItemUtil.isBackpack(hand) && id.equals(ItemUtil.getBackpackId(hand))) {
				ItemUtil.updateBackpackLore(hand, data);
				ItemUtil.syncShulkerPreview(hand, data);
			}
			refreshTopInventory(event.getView().getTopInventory(), id, data.getTier());
			player.updateInventory();
			return;
		}

		// Deposit from player inventory side with rich controls
		ItemStack moving = event.getCurrentItem();
		if (event.getClickedInventory() != player.getInventory()) return;
		if (moving == null || moving.getType() == Material.AIR) return;
		if (ItemUtil.isBackpack(moving)) return;

		BackpackTier tier = data.getTier();
		Material mat = moving.getType();
		int amountToDeposit = 0;
		switch (event.getClick()) {
			case LEFT -> amountToDeposit = 1;
			case RIGHT -> amountToDeposit = Math.min(64, moving.getAmount());
			case SHIFT_LEFT -> amountToDeposit = moving.getAmount();
			case SHIFT_RIGHT -> amountToDeposit = (moving.getAmount() + 1) / 2;
			case MIDDLE -> amountToDeposit = Math.min(32, moving.getAmount());
			case SWAP_OFFHAND -> {
				ItemStack off = player.getInventory().getItemInOffHand();
				if (off == null || off.getType() == Material.AIR) return;
				mat = off.getType();
				moving = off;
				amountToDeposit = off.getAmount();
				if (event.isShiftClick()) {
					// Also sweep same-type items across inventory
					amountToDeposit = sweepSameTypeFromInventory(player, mat) + amountToDeposit;
				}
			}
			default -> amountToDeposit = 0;
		}

		if (amountToDeposit <= 0) return;
		boolean isNewType = !data.getItemCounts().containsKey(mat);
		if (isNewType && data.totalItemTypes() >= tier.getStorageSlots()) {
			player.sendMessage(net.kyori.adventure.text.Component.text("Backpack is out of storage slots for new item types.").color(net.kyori.adventure.text.format.NamedTextColor.RED));
			return;
		}

        int transferred = Math.min(amountToDeposit, moving.getAmount());
        data.add(mat, transferred);
        moving.setAmount(moving.getAmount() - transferred);
		BackpackStorage.get().scheduleSave();

		// Update backpack item visuals
		ItemStack hand = player.getInventory().getItemInMainHand();
		if (ItemUtil.isBackpack(hand) && id.equals(ItemUtil.getBackpackId(hand))) {
			ItemUtil.updateBackpackLore(hand, data);
			ItemUtil.syncShulkerPreview(hand, data);
		}
		refreshTopInventory(event.getView().getTopInventory(), id, data.getTier());
		player.updateInventory();
	}



	@EventHandler
	public void onClose(InventoryCloseEvent event) {
		var holder = event.getView().getTopInventory().getHolder();
		if (!(holder instanceof com.axther.vexBags.gui.BackpackHolder)) return;
		BackpackStorage.get().scheduleSave();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		// Unlock all backpack recipes
		for (var tier : com.axther.vexBags.tier.BackpackTier.values()) {
			NamespacedKey key = com.axther.vexBags.util.ItemUtil.recipeKey(com.axther.vexBags.VexBags.getInstance(), tier);
			event.getPlayer().discoverRecipe(key);
		}
	}

	private void refreshTopInventory(Inventory inventory, UUID backpackId, BackpackTier tier) {
		BackpackData data = BackpackStorage.get().get(backpackId);
		if (data == null) return;
		inventory.clear();
		int slot = 0;
        for (var e : data.getItemCounts().entrySet()) {
			if (slot >= tier.getStorageSlots()) break;
			ItemStack display = new ItemStack(e.getKey());
			ItemMeta meta = display.getItemMeta();
			java.util.List<Component> lore = new java.util.ArrayList<>();
			lore.add(com.axther.vexBags.util.ItemUtil.mm().deserialize("<white>total: " + e.getValue() + "</white>"));
			lore.add(com.axther.vexBags.util.ItemUtil.CLICK_HINT);
			meta.lore(lore);
			display.setItemMeta(meta);
			display.setAmount(Math.min(e.getValue(), display.getMaxStackSize()));
			inventory.setItem(slot++, display);
		}
	}

	private void giveToPlayer(Player player, Material material, int amount) {
		int max = Math.max(1, material.getMaxStackSize());
		int remaining = amount;
		while (remaining > 0) {
			int give = Math.min(max, remaining);
			ItemStack stack = new ItemStack(material, give);
			player.getInventory().addItem(stack);
			remaining -= give;
		}
	}

	private int sweepSameTypeFromInventory(Player player, Material material) {
		int total = 0;
		for (int i = 0; i < player.getInventory().getSize(); i++) {
			ItemStack it = player.getInventory().getItem(i);
			if (it == null || it.getType() == Material.AIR) continue;
			if (ItemUtil.isBackpack(it)) continue;
			if (it.getType() != material) continue;
			total += it.getAmount();
			player.getInventory().setItem(i, null);
		}
		return total;
	}

    private int computeCapacityFor(Player player, Material material) {
        int max = Math.max(1, material.getMaxStackSize());
        int space = 0;
        // Offhand
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off == null || off.getType() == Material.AIR) {
            space += max;
        } else if (off.getType() == material) {
            space += Math.max(0, max - off.getAmount());
        }
        // Main inventory
        for (int i = 0; i < 36; i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it == null || it.getType() == Material.AIR) {
                space += max;
            } else if (it.getType() == material) {
                space += Math.max(0, max - it.getAmount());
            }
        }
        return space;
    }
}


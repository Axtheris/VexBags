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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
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
			String key = com.axther.vexBags.util.ItemUtil.stackKey(clicked);
			int available = data.getCountByKey(key);
			if (available <= 0) return;

			int desired = 0;
			switch (event.getClick()) {
				case LEFT -> desired = Math.min(1, available);
				case RIGHT -> desired = Math.min(8, available);
				case SHIFT_LEFT -> desired = Math.min(16, available);
				case SHIFT_RIGHT -> desired = Math.min(32, available);
				case MIDDLE -> desired = Math.min(64, available);
				case SWAP_OFFHAND -> desired = available; // F on inside: withdraw all
				default -> desired = 0;
			}

			if (desired <= 0) return;
			int removed = data.removeByKey(key, desired);
			if (removed > 0) giveToPlayer(player, clicked.getType(), removed);
			BackpackStorage.get().scheduleSave();

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

        // Deposit from player inventory side with rich controls (shift-click, F=deposit all, double-click store all)
		ItemStack moving = event.getCurrentItem();
		if (event.getClickedInventory() != player.getInventory()) return;
		if (moving == null || moving.getType() == Material.AIR) return;
		if (ItemUtil.isBackpack(moving)) return;

		BackpackTier tier = data.getTier();
        String mKey = com.axther.vexBags.util.ItemUtil.stackKey(moving);
        ClickType click = event.getClick();

        if (click == ClickType.SWAP_OFFHAND) {
            // F: deposit all of that type (sweep entire inventory for same key)
            int total = 0;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack it = player.getInventory().getItem(i);
                if (it == null || it.getType() == Material.AIR) continue;
                if (ItemUtil.isBackpack(it)) continue;
                if (!com.axther.vexBags.util.ItemUtil.stackKey(it).equals(mKey)) continue;
                int add = it.getAmount();
                boolean isNewType = data.getEntries().get(mKey) == null;
                if (isNewType && data.totalItemTypes() >= tier.getStorageSlots()) {
                    player.sendMessage(net.kyori.adventure.text.Component.text("Backpack is out of storage slots for new item types.").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                    break;
                }
                data.add(it, mKey, add);
                total += add;
                player.getInventory().setItem(i, null);
            }
            if (total > 0) {
                BackpackStorage.get().scheduleSave();
            }
        } else if (click == ClickType.DOUBLE_CLICK) {
            // Pick up + double-click: store all of same type from inventory
            ItemStack basis = event.getCursor();
            if (basis == null || basis.getType() == Material.AIR) basis = moving;
            if (basis == null || basis.getType() == Material.AIR) return;
            String key = com.axther.vexBags.util.ItemUtil.stackKey(basis);
            int total = 0;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack it = player.getInventory().getItem(i);
                if (it == null || it.getType() == Material.AIR) continue;
                if (ItemUtil.isBackpack(it)) continue;
                if (!com.axther.vexBags.util.ItemUtil.stackKey(it).equals(key)) continue;
                int add = it.getAmount();
                boolean isNewType = data.getEntries().get(key) == null;
                if (isNewType && data.totalItemTypes() >= tier.getStorageSlots()) {
                    player.sendMessage(net.kyori.adventure.text.Component.text("Backpack is out of storage slots for new item types.").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                    break;
                }
                data.add(it, key, add);
                total += add;
                player.getInventory().setItem(i, null);
            }
            // Clear cursor as if collected (compatible approach)
            player.setItemOnCursor(null);
            if (total > 0) {
                BackpackStorage.get().scheduleSave();
            }
        } else if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            // Quick-move like chest: deposit the whole stack
            int amount = moving.getAmount();
            boolean isNewType = data.getEntries().get(mKey) == null;
            if (isNewType && data.totalItemTypes() >= tier.getStorageSlots()) {
                player.sendMessage(net.kyori.adventure.text.Component.text("Backpack is out of storage slots for new item types.").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
            data.add(moving, mKey, amount);
            moving.setAmount(0);
            BackpackStorage.get().scheduleSave();
        } else {
            // Ignore plain left/right in player inventory (no custom deposit)
            return;
        }

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
        // Rediscover recipes with our namespace
        java.util.List<NamespacedKey> keys = new java.util.ArrayList<>();
        for (var tier : com.axther.vexBags.tier.BackpackTier.values()) {
            keys.add(com.axther.vexBags.util.ItemUtil.recipeKey(com.axther.vexBags.VexBags.getInstance(), tier));
        }
        event.getPlayer().discoverRecipes(keys);
	}

	private void refreshTopInventory(Inventory inventory, UUID backpackId, BackpackTier tier) {
		BackpackData data = BackpackStorage.get().get(backpackId);
		if (data == null) return;
		inventory.clear();
        int slot = 0;
        var sorted = new java.util.ArrayList<>(data.getEntries().values());
        sorted.sort((a,b) -> Integer.compare(b.getAmount(), a.getAmount()));
        for (var st : sorted) {
            if (slot >= tier.getStorageSlots()) break;
            ItemStack display = st.getTemplate().clone();
            ItemMeta meta = display.getItemMeta();
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(com.axther.vexBags.util.ItemUtil.mm().deserialize("<white>total: " + st.getAmount() + "</white>"));
            lore.add(com.axther.vexBags.util.ItemUtil.CLICK_HINT);
            meta.lore(lore);
            display.setItemMeta(meta);
            display.setAmount(Math.min(st.getAmount(), display.getMaxStackSize()));
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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


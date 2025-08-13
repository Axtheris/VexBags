package com.axther.vexBags.util;

import com.axther.vexBags.VexBags;
import com.axther.vexBags.storage.BackpackData;
import com.axther.vexBags.tier.BackpackTier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ItemUtil {

	private ItemUtil() {}

    // Cache MiniMessage instance and static components to reduce allocations
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static MiniMessage mm() { return MM; }
    public static final Component CLICK_HINT = noItalics(MM.deserialize("<gray>" + ItemUtil.toSmallCaps("withdraw: left 1 | right 8 | shift-left 16 | shift-right 32 | middle 64 | f all") + "</gray>"));

    public static Component prefix() {
        org.bukkit.configuration.file.FileConfiguration cfg = VexBags.getInstance().getConfig();
        boolean enabled = cfg.getBoolean("chat.prefix.enabled", true);
        String text = cfg.getString("chat.prefix.text", "vexbags");
        String start = cfg.getString("chat.prefix.gradient_start", com.axther.vexBags.tier.BackpackTier.NETHERITE.getHexColor());
        String end = cfg.getString("chat.prefix.gradient_end", com.axther.vexBags.tier.BackpackTier.DIAMOND.getHexColor());
        boolean brackets = cfg.getBoolean("chat.prefix.brackets", true);
        String separator = cfg.getString("chat.prefix.separator", " » ");
        StringBuilder sb = new StringBuilder();
        if (brackets) sb.append("<gray>[</gray>");
        sb.append("<gradient:#").append(normalizeHex(start)).append(":#").append(normalizeHex(end)).append(">")
                .append(toSmallCaps(text)).append("</gradient>");
        if (brackets) sb.append("<gray>]</gray>");
        sb.append("<dark_gray>").append(separator).append("</dark_gray>");
        Component built = noItalics(MM.deserialize(sb.toString()));
        return enabled ? built : Component.empty();
    }

    public static void sendPrefixed(CommandSender target, String miniMessage) {
        target.sendMessage(prefix().append(noItalics(MM.deserialize(miniMessage))));
    }

    public static void sendPrefixed(CommandSender target, Component component) {
        target.sendMessage(prefix().append(noItalics(component)));
    }

	public static boolean isBackpack(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) return false;
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return false;
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		NamespacedKey tierKey = VexBags.getInstance().getKeyBackpackTier();
        return pdc.has(tierKey, PersistentDataType.STRING);
	}

	public static BackpackTier getTier(ItemStack item) {
		if (item == null) return null;
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return null;
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		String tierName = pdc.get(VexBags.getInstance().getKeyBackpackTier(), PersistentDataType.STRING);
		if (tierName == null) return null;
		return BackpackTier.fromString(tierName);
	}

	public static UUID getBackpackId(ItemStack item) {
		if (item == null) return null;
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return null;
		String id = meta.getPersistentDataContainer().get(VexBags.getInstance().getKeyBackpackId(), PersistentDataType.STRING);
		if (id == null) return null;
		try { return UUID.fromString(id); } catch (IllegalArgumentException ex) { return null; }
	}

	public static void ensureBackpackHasId(ItemStack item) {
		if (!isBackpack(item)) return;
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		NamespacedKey idKey = VexBags.getInstance().getKeyBackpackId();
		if (!pdc.has(idKey, PersistentDataType.STRING)) {
			pdc.set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString());
			item.setItemMeta(meta);
		}
        signBackpack(item);
	}

	public static ItemStack createNewBackpackItem(BackpackTier tier) {
        ItemStack item = new ItemStack(tier.getBackpackMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.displayName(noItalics(MM.deserialize("<color:#" + normalizeHex(tier.getHexColor()) + ">" + toSmallCaps(tier.getDisplayName() + " backpack") + "</color>")));
        int cmd = tier.getCustomModelData();
        if (cmd > 0) meta.setCustomModelData(cmd);
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		pdc.set(VexBags.getInstance().getKeyBackpackTier(), PersistentDataType.STRING, tier.name());
		pdc.set(VexBags.getInstance().getKeyBackpackId(), PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(VexBags.getInstance().getKeyBackpackVersion(), PersistentDataType.INTEGER, 1);
        pdc.set(VexBags.getInstance().getKeyBackpackSession(), PersistentDataType.STRING, java.util.UUID.randomUUID().toString());
		item.setItemMeta(meta);
        updateBackpackLore(item, null);
        // Apply cosmetic overrides if configured
        var mgr = VexBags.getInstance().getIntegrations();
        if (mgr != null) {
            item = mgr.applyBackpackCosmetics(item, tier);
        }
        signBackpack(item);
		return item;
	}

	public static void updateBackpackLore(ItemStack item, BackpackData dataOrNull) {
		BackpackTier tier = getTier(item);
		if (tier == null) return;
		ItemMeta meta = item.getItemMeta();
		List<Component> lore = new ArrayList<>();
		String hex = normalizeHex(tier.getHexColor());
		String light = shiftColor(hex, 0.35);
		// Header divider
        lore.add(noItalics(MM.deserialize("<gradient:#" + light + ":#" + hex + "><bold>───────</bold></gradient>")));
		// Tier line
        lore.add(noItalics(MM.deserialize("<dark_gray>•</dark_gray> <gray>tier:</gray> <color:#" + hex + ">" + toSmallCaps(tier.getDisplayName()) + "</color>")));
		// Stats
        lore.add(noItalics(MM.deserialize("<dark_gray>•</dark_gray> <gray>upgrade slots:</gray> <white>" + tier.getUpgradeSlots() + "</white>")));
		BackpackData data = dataOrNull;
		if (data == null) {
			UUID id = getBackpackId(item);
			if (id != null) {
				data = com.axther.vexBags.storage.BackpackStorage.get().getOrCreate(id, tier);
			}
		}
		int unique = 0;
		int total = 0;
        if (data != null) {
            unique = data.getEntries().size();
            total = data.totalItems();
        }
        // Capacity and percentage (visuals)
        int maxCapacity = tier.getStorageSlots() * 64; // configurable later via config
        int percentFull = maxCapacity == 0 ? 0 : Math.min(100, (int)Math.round((total * 100.0) / maxCapacity));
        int percentMax = 100;
        String bar = buildProgressBar(unique, tier.getStorageSlots(), 10, hex);
        lore.add(noItalics(MM.deserialize("<dark_gray>•</dark_gray> <gray>" + ItemUtil.toSmallCaps("storage slots:") + "</gray> <white>" + unique + "/" + tier.getStorageSlots() + "</white>  " + bar)));
        lore.add(noItalics(MM.deserialize("<dark_gray>•</dark_gray> <gray>" + ItemUtil.toSmallCaps("capacity:") + "</gray> <white>" + total + "/" + maxCapacity + "</white> <dark_gray>(</dark_gray><gray>" + percentFull + "</gray><white>/</white><gray>" + percentMax + "</gray><dark_gray>)</dark_gray>")));
        lore.add(noItalics(MM.deserialize("<gray>" + ItemUtil.toSmallCaps("deposit keys:") + "</gray> <white>" + ItemUtil.toSmallCaps("shift-click to quick-move, f = deposit all, pick up + double-click = store all") + "</white>")));
		// Spacer
        lore.add(noItalics(Component.text(" ")));
        // Top items by count
        if (data != null && !data.getEntries().isEmpty()) {
            lore.add(noItalics(MM.deserialize("<gray>" + ItemUtil.toSmallCaps("top items:") + "</gray>")));
            data.getEntries().values().stream()
                    .sorted((a,b) -> Integer.compare(b.getAmount(), a.getAmount()))
                    .limit(5)
                    .forEach(st -> lore.add(noItalics(MM.deserialize("<dark_gray>›</dark_gray> <white>" + st.getTemplate().getType().name().toLowerCase() + "</white> <gray>×</gray> <white>" + st.getAmount() + "</white>"))));
        }
		// Footer divider
        lore.add(noItalics(MM.deserialize("<gradient:#" + hex + ":#" + light + "><bold>───────</bold></gradient>")));
		meta.lore(lore);
		item.setItemMeta(meta);
	}

    public static void registerRecipeForTier(VexBags plugin, BackpackTier tier) {
		// Use a shell (no ID) as recipe result; assign IDs during CraftItemEvent
		ItemStack shellResult = createBackpackShell(tier);
		ShapedRecipe recipe = new ShapedRecipe(recipeKey(plugin, tier), shellResult);
        if (tier == BackpackTier.LEATHER) {
			recipe.shape("LLL", "LCL", "LLL");
			recipe.setIngredient('L', Material.LEATHER);
			recipe.setIngredient('C', Material.CHEST);
		} else {
			recipe.shape("III", "IBI", "III");
			recipe.setIngredient('I', new RecipeChoice.MaterialChoice(tier.getUpgradeMaterial()));
			// Allow any bundle in the recipe, validate in PrepareItemCraftEvent that it's our prev-tier backpack
            recipe.setIngredient('B', new RecipeChoice.MaterialChoice(allShulkerBoxMaterials()));
		}
		Bukkit.addRecipe(recipe);
	}

	public static NamespacedKey recipeKey(VexBags plugin, BackpackTier tier) {
		return new NamespacedKey(plugin, "backpack_" + tier.name().toLowerCase());
	}

	private static ItemStack createBackpackShell(BackpackTier tier) {
        ItemStack shell = new ItemStack(tier.getBackpackMaterial());
        ItemMeta meta = shell.getItemMeta();
        meta.displayName(noItalics(MM.deserialize("<color:#" + normalizeHex(tier.getHexColor()) + ">" + toSmallCaps(tier.getDisplayName() + " backpack") + "</color>")));
        int cmd = tier.getCustomModelData();
        if (cmd > 0) meta.setCustomModelData(cmd);
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		pdc.set(VexBags.getInstance().getKeyBackpackTier(), PersistentDataType.STRING, tier.name());
		// Note: NO ID here, it will be assigned on craft
		shell.setItemMeta(meta);
		updateBackpackLore(shell, null);
        signBackpack(shell);
		return shell;
	}

    public static String toSmallCaps(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                sb.append(switch (c) {
                    case 'a' -> 'ᴀ'; case 'b' -> 'ʙ'; case 'c' -> 'ᴄ'; case 'd' -> 'ᴅ'; case 'e' -> 'ᴇ';
                    case 'f' -> 'ꜰ'; case 'g' -> 'ɢ'; case 'h' -> 'ʜ'; case 'i' -> 'ɪ'; case 'j' -> 'ᴊ';
                    case 'k' -> 'ᴋ'; case 'l' -> 'ʟ'; case 'm' -> 'ᴍ'; case 'n' -> 'ɴ'; case 'o' -> 'ᴏ';
                    case 'p' -> 'ᴘ'; case 'q' -> 'ǫ'; case 'r' -> 'ʀ'; case 's' -> 's'; case 't' -> 'ᴛ';
                    case 'u' -> 'ᴜ'; case 'v' -> 'ᴠ'; case 'w' -> 'ᴡ'; case 'x' -> 'x'; case 'y' -> 'ʏ';
                    case 'z' -> 'ᴢ'; default -> c;
                });
            } else if (c >= 'A' && c <= 'Z') {
                char lower = Character.toLowerCase(c);
                sb.append(toSmallCaps(String.valueOf(lower)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String normalizeHex(String hex) {
        if (hex == null) return "ffffff";
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        return h;
    }

	private static String shiftColor(String hex, double lightenAmount) {
		String h = normalizeHex(hex);
		int r = Integer.parseInt(h.substring(0, 2), 16);
		int g = Integer.parseInt(h.substring(2, 4), 16);
		int b = Integer.parseInt(h.substring(4, 6), 16);
		r = (int) Math.max(0, Math.min(255, r + (255 - r) * lightenAmount));
		g = (int) Math.max(0, Math.min(255, g + (255 - g) * lightenAmount));
		b = (int) Math.max(0, Math.min(255, b + (255 - b) * lightenAmount));
		return String.format("%02x%02x%02x", r, g, b);
	}

	private static String buildProgressBar(int value, int max, int segments, String colorHex) {
		if (max <= 0) max = 1;
		int filled = (int) Math.round((value / (double) max) * segments);
		filled = Math.max(0, Math.min(segments, filled));
		int empty = segments - filled;
		StringBuilder sb = new StringBuilder();
		sb.append("<color:#").append(normalizeHex(colorHex)).append(">");
		for (int i = 0; i < filled; i++) sb.append("■");
		sb.append("</color><dark_gray>");
		for (int i = 0; i < empty; i++) sb.append("■");
		sb.append("</dark_gray>");
		return sb.toString();
	}

    private static Component noItalics(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

	public static void setBackpackId(ItemStack item, UUID id) {
		if (item == null) return;
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return;
		meta.getPersistentDataContainer().set(VexBags.getInstance().getKeyBackpackId(), PersistentDataType.STRING, id.toString());
		item.setItemMeta(meta);
        signBackpack(item);
	}

    public static Material[] allShulkerBoxMaterials() {
        return new Material[] {
                Material.SHULKER_BOX,
                Material.WHITE_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.BLACK_SHULKER_BOX,
                Material.BROWN_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX,
                Material.LIME_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX,
                Material.BLUE_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.PINK_SHULKER_BOX
        };
    }

    public static Material dyeToShulker(Material dye) {
        switch (dye) {
            case WHITE_DYE: return Material.WHITE_SHULKER_BOX;
            case LIGHT_GRAY_DYE: return Material.LIGHT_GRAY_SHULKER_BOX;
            case GRAY_DYE: return Material.GRAY_SHULKER_BOX;
            case BLACK_DYE: return Material.BLACK_SHULKER_BOX;
            case BROWN_DYE: return Material.BROWN_SHULKER_BOX;
            case RED_DYE: return Material.RED_SHULKER_BOX;
            case ORANGE_DYE: return Material.ORANGE_SHULKER_BOX;
            case YELLOW_DYE: return Material.YELLOW_SHULKER_BOX;
            case LIME_DYE: return Material.LIME_SHULKER_BOX;
            case GREEN_DYE: return Material.GREEN_SHULKER_BOX;
            case CYAN_DYE: return Material.CYAN_SHULKER_BOX;
            case LIGHT_BLUE_DYE: return Material.LIGHT_BLUE_SHULKER_BOX;
            case BLUE_DYE: return Material.BLUE_SHULKER_BOX;
            case PURPLE_DYE: return Material.PURPLE_SHULKER_BOX;
            case MAGENTA_DYE: return Material.MAGENTA_SHULKER_BOX;
            case PINK_DYE: return Material.PINK_SHULKER_BOX;
            default: return null;
        }
    }

    public static void syncShulkerPreview(ItemStack backpack, BackpackData data) {
        if (backpack == null || data == null) return;
        ItemMeta meta = backpack.getItemMeta();
        if (!(meta instanceof BlockStateMeta)) return;
        BlockStateMeta bsm = (BlockStateMeta) meta;
        if (!(bsm.getBlockState() instanceof ShulkerBox)) return;
        ShulkerBox box = (ShulkerBox) bsm.getBlockState();
        box.getInventory().clear();
        // Fill preview inventory with stacks representing totals
        int slot = 0;
        for (var e : data.getEntries().entrySet()) {
            var stored = e.getValue();
            int remaining = stored.getAmount();
            while (remaining > 0 && slot < box.getInventory().getSize()) {
                ItemStack template = stored.getTemplate();
                int max = Math.max(1, template.getMaxStackSize());
                int give = Math.min(max, remaining);
                ItemStack display = template.clone();
                display.setAmount(give);
                box.getInventory().setItem(slot++, display);
                remaining -= give;
            }
            if (slot >= box.getInventory().getSize()) break;
        }
        bsm.setBlockState(box);
        backpack.setItemMeta(bsm);
    }

    public static String stackKey(ItemStack stack) {
        if (stack == null) return "null";
        ItemStack one = stack.clone();
        one.setAmount(1);
        java.util.Map<String,Object> serial = one.serialize();
        String canonical = canonicalString(serial);
        return one.getType().name() + "|" + sha256(canonical);
    }

    private static String canonicalString(Object o) {
        if (o == null) return "null";
        if (o instanceof java.util.Map<?,?> map) {
            java.util.TreeMap<String,String> sorted = new java.util.TreeMap<>();
            for (var e : map.entrySet()) {
                sorted.put(String.valueOf(e.getKey()), canonicalString(e.getValue()));
            }
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (var e : sorted.entrySet()) {
                if (!first) sb.append(','); first = false;
                sb.append(e.getKey()).append(':').append(e.getValue());
            }
            return sb.append('}').toString();
        }
        if (o instanceof java.util.List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i=0;i<list.size();i++) {
                if (i>0) sb.append(',');
                sb.append(canonicalString(list.get(i)));
            }
            return sb.append(']').toString();
        }
        return String.valueOf(o);
    }

    private static String sha256(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    // Dupe protection: sign with secret + id + version + session
    public static void signBackpack(ItemStack item) {
        if (!isBackpack(item)) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(VexBags.getInstance().getKeyBackpackId(), PersistentDataType.STRING);
        if (id == null) return;
        Integer ver = pdc.get(VexBags.getInstance().getKeyBackpackVersion(), PersistentDataType.INTEGER);
        if (ver == null) ver = 1;
        String sess = pdc.get(VexBags.getInstance().getKeyBackpackSession(), PersistentDataType.STRING);
        if (sess == null) sess = java.util.UUID.randomUUID().toString();
        String secret = VexBags.getInstance().getServerSecret();
        String payload = id + ":" + ver + ":" + sess + ":" + secret;
        String sig = Integer.toHexString(payload.hashCode());
        pdc.set(VexBags.getInstance().getKeyBackpackSig(), PersistentDataType.STRING, sig);
        item.setItemMeta(meta);
    }

    public static boolean verifyBackpackSignature(ItemStack item) {
        if (!isBackpack(item)) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(VexBags.getInstance().getKeyBackpackId(), PersistentDataType.STRING);
        String sess = pdc.get(VexBags.getInstance().getKeyBackpackSession(), PersistentDataType.STRING);
        Integer ver = pdc.get(VexBags.getInstance().getKeyBackpackVersion(), PersistentDataType.INTEGER);
        String sig = pdc.get(VexBags.getInstance().getKeyBackpackSig(), PersistentDataType.STRING);
        if (id == null || sess == null || ver == null || sig == null) return false;
        String secret = VexBags.getInstance().getServerSecret();
        String payload = id + ":" + ver + ":" + sess + ":" + secret;
        String expected = Integer.toHexString(payload.hashCode());
        return expected.equals(sig);
    }

    // removed duplicate overload
}


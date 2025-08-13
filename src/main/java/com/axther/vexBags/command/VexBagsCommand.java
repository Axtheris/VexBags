package com.axther.vexBags.command;

import com.axther.vexBags.storage.BackpackStorage;
import com.axther.vexBags.tier.BackpackTier;
import com.axther.vexBags.util.ItemUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VexBagsCommand implements CommandExecutor, org.bukkit.command.TabCompleter {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Players only.");
			return true;
		}
		BackpackTier tier = BackpackTier.LEATHER;
		if (args.length > 0) {
			BackpackTier maybe = BackpackTier.fromString(args[0]);
			if (maybe != null) tier = maybe;
		}
		ItemStack bp = ItemUtil.createNewBackpackItem(tier);
		// Set owner to issuing player for indexing
		var id = ItemUtil.getBackpackId(bp);
		if (id != null) {
			BackpackStorage.get().getOrCreate(id, tier).setOwnerId(player.getUniqueId());
			BackpackStorage.get().scheduleSave();
		}
        player.getInventory().addItem(bp);
        com.axther.vexBags.util.ItemUtil.sendPrefixed(player, "<gray>gave you a </gray><color:#" + com.axther.vexBags.util.ItemUtil.normalizeHex(tier.getHexColor()) + ">" + com.axther.vexBags.util.ItemUtil.toSmallCaps(tier.getDisplayName() + " backpack") + "</color>");
		return true;
	}

	@Override
	public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return java.util.Arrays.stream(com.axther.vexBags.tier.BackpackTier.values())
					.map(Enum::name)
					.map(String::toLowerCase)
					.filter(s -> s.startsWith(args[0].toLowerCase()))
					.toList();
		}
		return java.util.Collections.emptyList();
	}
}


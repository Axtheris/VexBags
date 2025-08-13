package com.axther.vexBags.command;

import com.axther.vexBags.gui.BackpackGui;
import com.axther.vexBags.storage.BackpackData;
import com.axther.vexBags.storage.BackpackStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class AdminBackpackCommand implements CommandExecutor, org.bukkit.command.TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vexbags.admin")) {
            com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>No permission.</red>");
            return true;
        }
        if (args.length == 0) {
            com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>Usage:</gray> <white>/vexbagsadmin <list|open|delete|restore> [player|uuid]</white>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                com.axther.vexBags.VexBags.getInstance().reloadPluginConfigAndSecret();
                com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>Configuration reloaded.</gray>");
                return true;
            case "list":
                if (args.length < 2) {
                    com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>Usage:</gray> <white>/vexbagsadmin list <player></white>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
                if (target == null) target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null || target.getUniqueId() == null) {
                    com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Player not found.</red>");
                    return true;
                }
                UUID pid = target.getUniqueId();
                int count = 0;
                for (Map.Entry<UUID, BackpackData> e : BackpackStorage.get().all().entrySet()) {
                    BackpackData data = e.getValue();
                    if (pid.equals(data.getOwnerId())) {
                        sender.sendMessage("- " + e.getKey() + " (" + data.getTier().name() + ") items=" + data.totalItems());
                        count++;
                    }
                }
                if (count == 0) com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>No backpacks for that player.</gray>");
                return true;

            case "open":
                if (!(sender instanceof Player admin)) {
                    com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Only players can open a backpack.</red>");
                    return true;
                }
                if (args.length < 2) {
                    com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>Usage:</gray> <white>/vexbagsadmin open <uuid></white>");
                    return true;
                }
                try {
                    UUID id = UUID.fromString(args[1]);
                    BackpackData data = BackpackStorage.get().get(id);
                    if (data == null) { com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Backpack not found.</red>"); return true; }
                    BackpackGui gui = new BackpackGui(id, data.getTier());
                    gui.open(admin);
                } catch (IllegalArgumentException ex) {
                    com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Invalid UUID.</red>");
                }
                return true;

            case "delete":
                if (args.length < 2) { com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>Usage:</gray> <white>/vexbagsadmin delete <uuid></white>"); return true; }
                try {
                    UUID id = UUID.fromString(args[1]);
                    boolean ok = BackpackStorage.get().deleteBackpack(id);
                    if (!ok) { com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Backpack not found.</red>"); return true; }
                    com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>Backpack deleted:</gray> <white>" + id + "</white>");
                } catch (IllegalArgumentException ex) { com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Invalid UUID.</red>"); }
                return true;

            case "restore":
                if (args.length < 3) {
                    com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>Usage:</gray> <white>/vexbagsadmin restore <uuid> <player></white>");
                    return true;
                }
                try {
                    UUID id = UUID.fromString(args[1]);
                    Player targetPlayer = Bukkit.getPlayer(args[2]);
                    if (targetPlayer == null) { com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Player not online.</red>"); return true; }
                    BackpackData data = BackpackStorage.get().get(id);
                    if (data == null) { com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Backpack not found.</red>"); return true; }
                    // Charge if configured
                    var mgr = com.axther.vexBags.VexBags.getInstance().getIntegrations();
                    if (mgr != null) {
                        boolean ok = mgr.chargeForAdminRestore(targetPlayer);
                        if (!ok) { com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Insufficient funds.</red>"); return true; }
                    }
                    // Give a new item pointing to the same backpack id
                    var item = com.axther.vexBags.util.ItemUtil.createNewBackpackItem(data.getTier());
                    com.axther.vexBags.util.ItemUtil.setBackpackId(item, id);
                    // Re-sign with current secret to prevent stale signatures after secret change
                    com.axther.vexBags.util.ItemUtil.signBackpack(item);
                    BackpackStorage.get().setOwner(id, targetPlayer.getUniqueId());
                    targetPlayer.getInventory().addItem(item);
                    com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>Restored backpack to</gray> <white>" + targetPlayer.getName() + "</white>");
                } catch (IllegalArgumentException ex) { com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<red>Invalid UUID.</red>"); }
                return true;
        }

        com.axther.vexBags.util.ItemUtil.sendPrefixed(sender, "<gray>Unknown subcommand.</gray>");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("vexbags.admin")) return java.util.Collections.emptyList();
        if (args.length == 1) {
            return java.util.stream.Stream.of("list", "open", "delete", "restore")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "open", "delete" -> {
                    return BackpackStorage.get().all().keySet().stream()
                            .map(UUID::toString)
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .limit(50)
                            .toList();
                }
                case "list", "restore" -> {
                    return java.util.Arrays.stream(Bukkit.getOfflinePlayers())
                            .map(p -> p.getName() == null ? p.getUniqueId().toString() : p.getName())
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .limit(50)
                            .toList();
                }
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("restore")) {
            return java.util.Arrays.stream(Bukkit.getOnlinePlayers().toArray(Player[]::new))
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(50)
                    .toList();
        }
        return java.util.Collections.emptyList();
    }
}



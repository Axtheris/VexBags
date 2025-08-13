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
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /vexbagsadmin <list|open|delete|restore> [player|uuid]");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /vexbagsadmin list <player>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
                if (target == null) target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null || target.getUniqueId() == null) {
                    sender.sendMessage("Player not found.");
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
                if (count == 0) sender.sendMessage("No backpacks for that player.");
                return true;

            case "open":
                if (!(sender instanceof Player admin)) {
                    sender.sendMessage("Only players can open a backpack.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /vexbagsadmin open <uuid>");
                    return true;
                }
                try {
                    UUID id = UUID.fromString(args[1]);
                    BackpackData data = BackpackStorage.get().get(id);
                    if (data == null) { sender.sendMessage("Backpack not found."); return true; }
                    BackpackGui gui = new BackpackGui(id, data.getTier());
                    gui.open(admin);
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid UUID.");
                }
                return true;

            case "delete":
                if (args.length < 2) { sender.sendMessage("Usage: /vexbagsadmin delete <uuid>"); return true; }
                try {
                    UUID id = UUID.fromString(args[1]);
                    Map<UUID, BackpackData> all = BackpackStorage.get().all();
                    BackpackData removed = all.remove(id);
                    if (removed == null) { sender.sendMessage("Backpack not found."); return true; }
                    // Reflect removal in storage
                    BackpackStorage.get().all().remove(id);
                    sender.sendMessage("Backpack deleted: " + id);
                    BackpackStorage.get().scheduleSave();
                } catch (IllegalArgumentException ex) { sender.sendMessage("Invalid UUID."); }
                return true;

            case "restore":
                if (args.length < 3) {
                    sender.sendMessage("Usage: /vexbagsadmin restore <uuid> <player>");
                    return true;
                }
                try {
                    UUID id = UUID.fromString(args[1]);
                    Player targetPlayer = Bukkit.getPlayer(args[2]);
                    if (targetPlayer == null) { sender.sendMessage("Player not online."); return true; }
                    BackpackData data = BackpackStorage.get().get(id);
                    if (data == null) { sender.sendMessage("Backpack not found."); return true; }
                    // Give a new item pointing to the same backpack id
                    var item = com.axther.vexBags.util.ItemUtil.createNewBackpackItem(data.getTier());
                    com.axther.vexBags.util.ItemUtil.setBackpackId(item, id);
                    BackpackStorage.get().setOwner(id, targetPlayer.getUniqueId());
                    targetPlayer.getInventory().addItem(item);
                    sender.sendMessage("Restored backpack to " + targetPlayer.getName());
                } catch (IllegalArgumentException ex) { sender.sendMessage("Invalid UUID."); }
                return true;
        }

        sender.sendMessage("Unknown subcommand.");
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



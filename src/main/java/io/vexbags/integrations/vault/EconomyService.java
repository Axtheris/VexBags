package io.vexbags.integrations.vault;

import com.axther.vexBags.VexBags;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class EconomyService {
    private final VexBags plugin;
    private Object economy; // net.milkbowl.vault.economy.Economy

    public EconomyService(VexBags plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        try {
            Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object rsp = Bukkit.getServicesManager().getRegistration(econClass);
            if (rsp != null) {
                this.economy = rsp.getClass().getMethod("getProvider").invoke(rsp);
            }
        } catch (Throwable ignored) {}
    }

    public boolean isAvailable() { return economy != null; }

    public boolean withdraw(Player player, double amount) {
        if (economy == null || player == null || amount <= 0) return true;
        try {
            Object response = economy.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class)
                    .invoke(economy, player, amount);
            if (response == null) return false;
            return (boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
        } catch (NoSuchMethodException ex) {
            try {
                Object response = economy.getClass().getMethod("withdrawPlayer", Player.class, double.class)
                        .invoke(economy, player, amount);
                if (response == null) return false;
                return (boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
            } catch (Throwable t) { return true; }
        } catch (Throwable t) {
            return true;
        }
    }
}



package io.vexbags.integrations;

import com.axther.vexBags.VexBags;
import com.axther.vexBags.tier.BackpackTier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IntegrationsManager {

    private final VexBags plugin;

    private boolean masterEnabled;
    private boolean debug;

    private boolean vaultEnabledCfg;
    private boolean papiEnabledCfg;
    private boolean wgEnabledCfg;
    private boolean clxEnabledCfg;
    private boolean itemsAdderEnabledCfg;
    private boolean oraxenEnabledCfg;
    private boolean townyEnabledCfg;
    private boolean griefPreventionEnabledCfg;

    private io.vexbags.integrations.vault.EconomyService economyService;
    private io.vexbags.integrations.placeholderapi.VexBagsExpansion papiExpansion;
    private io.vexbags.integrations.worldguard.WorldGuardHook worldGuardHook;
    private io.vexbags.integrations.combatlogx.CombatLogXHook combatLogXHook;
    private io.vexbags.integrations.itemsadder.ItemsAdderHook itemsAdderHook;
    private io.vexbags.integrations.oraxen.OraxenHook oraxenHook;
    private io.vexbags.integrations.towny.TownyHook townyHook;
    private io.vexbags.integrations.griefprevention.GriefPreventionHook griefPreventionHook;

    private final List<String> activeIntegrations = new ArrayList<>();

    public IntegrationsManager(VexBags plugin) { this.plugin = plugin; }

    public void init() {
        readConfigWithDefaults();
        if (!masterEnabled) { logHookSummary(); return; }

        if (vaultEnabledCfg && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            this.economyService = new io.vexbags.integrations.vault.EconomyService(plugin);
            if (economyService.isAvailable()) activeIntegrations.add("Vault");
        }
        if (papiEnabledCfg && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                this.papiExpansion = new io.vexbags.integrations.placeholderapi.VexBagsExpansion(plugin);
                if (this.papiExpansion.register()) activeIntegrations.add("PlaceholderAPI");
            } catch (Throwable ignored) {}
        }
        if (wgEnabledCfg && Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                this.worldGuardHook = new io.vexbags.integrations.worldguard.WorldGuardHook(plugin);
                this.worldGuardHook.registerFlag();
                activeIntegrations.add("WorldGuard");
            } catch (Throwable ignored) {}
        }
        if (clxEnabledCfg && Bukkit.getPluginManager().getPlugin("CombatLogX") != null) {
            try {
                boolean closeOnTag = plugin.getConfig().getBoolean("integrations.combatlogx.close_on_tag", true);
                this.combatLogXHook = new io.vexbags.integrations.combatlogx.CombatLogXHook(plugin, closeOnTag);
                this.combatLogXHook.registerDynamicListeners();
                activeIntegrations.add("CombatLogX");
            } catch (Throwable ignored) {}
        }
        if (itemsAdderEnabledCfg && Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            this.itemsAdderHook = new io.vexbags.integrations.itemsadder.ItemsAdderHook(plugin);
            activeIntegrations.add("ItemsAdder");
        }
        if (oraxenEnabledCfg && Bukkit.getPluginManager().getPlugin("Oraxen") != null) {
            this.oraxenHook = new io.vexbags.integrations.oraxen.OraxenHook(plugin);
            activeIntegrations.add("Oraxen");
        }
        if (townyEnabledCfg && Bukkit.getPluginManager().getPlugin("Towny") != null) {
            this.townyHook = new io.vexbags.integrations.towny.TownyHook(plugin);
            activeIntegrations.add("Towny");
        }
        if (griefPreventionEnabledCfg && Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
            this.griefPreventionHook = new io.vexbags.integrations.griefprevention.GriefPreventionHook(plugin);
            activeIntegrations.add("GriefPrevention");
        }
        logHookSummary();
    }

    public void reload() {
        if (this.papiExpansion != null) { try { this.papiExpansion.unregister(); } catch (Throwable ignored) {} this.papiExpansion = null; }
        if (this.combatLogXHook != null) { try { this.combatLogXHook.unregisterDynamicListeners(); } catch (Throwable ignored) {} this.combatLogXHook = null; }
        this.worldGuardHook = null;
        this.itemsAdderHook = null;
        this.oraxenHook = null;
        this.townyHook = null;
        this.griefPreventionHook = null;
        this.economyService = null;
        this.activeIntegrations.clear();
        init();
    }

    public void shutdown() {
        if (this.papiExpansion != null) { try { this.papiExpansion.unregister(); } catch (Throwable ignored) {} this.papiExpansion = null; }
        if (this.combatLogXHook != null) { try { this.combatLogXHook.unregisterDynamicListeners(); } catch (Throwable ignored) {} this.combatLogXHook = null; }
        this.worldGuardHook = null;
        this.itemsAdderHook = null;
        this.oraxenHook = null;
        this.townyHook = null;
        this.griefPreventionHook = null;
        this.economyService = null;
        this.activeIntegrations.clear();
    }

    private void readConfigWithDefaults() {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        cfg.addDefault("integrations.enabled", true);
        cfg.addDefault("integrations.debug", false);
        cfg.addDefault("integrations.vault.enabled", false);
        cfg.addDefault("integrations.vault.costs.upgrade_per_tier.leather", 0.0);
        cfg.addDefault("integrations.vault.costs.upgrade_per_tier.copper", 0.0);
        cfg.addDefault("integrations.vault.costs.upgrade_per_tier.iron", 0.0);
        cfg.addDefault("integrations.vault.costs.upgrade_per_tier.gold", 0.0);
        cfg.addDefault("integrations.vault.costs.upgrade_per_tier.diamond", 0.0);
        cfg.addDefault("integrations.vault.costs.upgrade_per_tier.netherite", 0.0);
        cfg.addDefault("integrations.vault.costs.admin_restore", 0.0);
        cfg.addDefault("integrations.placeholderapi.enabled", true);
        cfg.addDefault("integrations.worldguard.enabled", true);
        cfg.addDefault("integrations.worldguard.flag_default", "ALLOW");
        cfg.addDefault("integrations.combatlogx.enabled", true);
        cfg.addDefault("integrations.combatlogx.close_on_tag", true);
        cfg.addDefault("integrations.itemsadder.enabled", false);
        cfg.addDefault("integrations.oraxen.enabled", false);
        cfg.addDefault("integrations.towny.enabled", true);
        cfg.addDefault("integrations.towny.require_container_trust", true);
        cfg.addDefault("integrations.griefprevention.enabled", true);
        cfg.addDefault("integrations.griefprevention.require_container_trust", true);
        cfg.options().copyDefaults(true);
        plugin.saveConfig();

        this.masterEnabled = cfg.getBoolean("integrations.enabled", true);
        this.debug = cfg.getBoolean("integrations.debug", false);
        this.vaultEnabledCfg = cfg.getBoolean("integrations.vault.enabled", false);
        this.papiEnabledCfg = cfg.getBoolean("integrations.placeholderapi.enabled", true);
        this.wgEnabledCfg = cfg.getBoolean("integrations.worldguard.enabled", true);
        this.clxEnabledCfg = cfg.getBoolean("integrations.combatlogx.enabled", true);
        this.itemsAdderEnabledCfg = cfg.getBoolean("integrations.itemsadder.enabled", false);
        this.oraxenEnabledCfg = cfg.getBoolean("integrations.oraxen.enabled", false);
        this.townyEnabledCfg = cfg.getBoolean("integrations.towny.enabled", true);
        this.griefPreventionEnabledCfg = cfg.getBoolean("integrations.griefprevention.enabled", true);
    }

    private void logHookSummary() {
        if (!masterEnabled) return;
        Bukkit.getLogger().info("VexBags: Hooked into " + activeIntegrations.size() + " integrations");
        if (debug && !activeIntegrations.isEmpty()) Bukkit.getLogger().info(String.join(", ", activeIntegrations));
    }

    public boolean isMasterEnabled() { return masterEnabled; }

    public boolean isAllowedToOpen(Player player, Location location) {
        if (!masterEnabled) return true;
        if (combatLogXHook != null && combatLogXHook.isTagged(player)) return false;
        if (worldGuardHook != null && !worldGuardHook.isAllowedAt(player, location)) return false;
        if (townyHook != null) {
            boolean requireContainerTrust = plugin.getConfig().getBoolean("integrations.towny.require_container_trust", true);
            if (!townyHook.hasRequiredTrust(player, location, requireContainerTrust)) return false;
        }
        if (griefPreventionHook != null) {
            boolean requireContainerTrust = plugin.getConfig().getBoolean("integrations.griefprevention.require_container_trust", true);
            if (!griefPreventionHook.hasContainerTrust(player, location, requireContainerTrust)) return false;
        }
        return true;
    }

    public ItemStack applyBackpackCosmetics(ItemStack base, BackpackTier tier) {
        if (!masterEnabled) return base;
        try {
            if (oraxenHook != null) {
                ItemStack custom = oraxenHook.itemForTier(tier);
                if (custom != null) return mergeCosmetic(base, custom);
            }
            if (itemsAdderHook != null) {
                ItemStack custom = itemsAdderHook.itemForTier(tier);
                if (custom != null) return mergeCosmetic(base, custom);
            }
            Integer cmd = null;
            if (oraxenHook != null) cmd = getIntegrationCMD("integrations.oraxen", tier);
            if (cmd == null && itemsAdderHook != null) cmd = getIntegrationCMD("integrations.itemsadder", tier);
            if (cmd != null && cmd > 0) {
                ItemMeta meta = base.getItemMeta();
                meta.setCustomModelData(cmd);
                base.setItemMeta(meta);
            }
        } catch (Throwable ignored) {}
        return base;
    }

    private Integer getIntegrationCMD(String root, BackpackTier tier) {
        String path = root + ".tiers." + tier.name().toLowerCase() + ".custom_model_data";
        if (!plugin.getConfig().isInt(path)) return null;
        int v = plugin.getConfig().getInt(path, 0);
        return v <= 0 ? null : v;
    }

    private ItemStack mergeCosmetic(ItemStack base, ItemStack cosmetic) {
        ItemMeta baseMeta = base.getItemMeta();
        var pdc = baseMeta.getPersistentDataContainer();
        String id = pdc.get(VexBags.getInstance().getKeyBackpackId(), org.bukkit.persistence.PersistentDataType.STRING);
        String tier = pdc.get(VexBags.getInstance().getKeyBackpackTier(), org.bukkit.persistence.PersistentDataType.STRING);
        Integer ver = pdc.get(VexBags.getInstance().getKeyBackpackVersion(), org.bukkit.persistence.PersistentDataType.INTEGER);
        String sess = pdc.get(VexBags.getInstance().getKeyBackpackSession(), org.bukkit.persistence.PersistentDataType.STRING);
        String sig = pdc.get(VexBags.getInstance().getKeyBackpackSig(), org.bukkit.persistence.PersistentDataType.STRING);

        ItemStack result = cosmetic.clone();
        result.setAmount(1);
        ItemMeta cm = result.getItemMeta();
        cm.displayName(baseMeta.displayName());
        cm.lore(baseMeta.lore());
        var rpdc = cm.getPersistentDataContainer();
        if (tier != null) rpdc.set(VexBags.getInstance().getKeyBackpackTier(), org.bukkit.persistence.PersistentDataType.STRING, tier);
        if (id != null) rpdc.set(VexBags.getInstance().getKeyBackpackId(), org.bukkit.persistence.PersistentDataType.STRING, id);
        if (ver != null) rpdc.set(VexBags.getInstance().getKeyBackpackVersion(), org.bukkit.persistence.PersistentDataType.INTEGER, ver);
        if (sess != null) rpdc.set(VexBags.getInstance().getKeyBackpackSession(), org.bukkit.persistence.PersistentDataType.STRING, sess);
        if (sig != null) rpdc.set(VexBags.getInstance().getKeyBackpackSig(), org.bukkit.persistence.PersistentDataType.STRING, sig);
        result.setItemMeta(cm);
        return result;
    }

    public boolean chargeForUpgrade(Player player, BackpackTier targetTier) {
        if (!masterEnabled) return true;
        if (economyService == null || !economyService.isAvailable()) return true;
        double cost = plugin.getConfig().getDouble("integrations.vault.costs.upgrade_per_tier." + targetTier.name().toLowerCase(), 0.0);
        if (cost <= 0.0) return true;
        return economyService.withdraw(player, cost);
    }

    public boolean chargeForAdminRestore(Player player) {
        if (!masterEnabled) return true;
        if (economyService == null || !economyService.isAvailable()) return true;
        double cost = plugin.getConfig().getDouble("integrations.vault.costs.admin_restore", 0.0);
        if (cost <= 0.0) return true;
        return economyService.withdraw(player, cost);
    }

    public List<String> getActiveIntegrations() { return Collections.unmodifiableList(activeIntegrations); }
}



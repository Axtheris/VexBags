package com.axther.vexBags;

import com.axther.vexBags.command.VexBagsCommand;
import com.axther.vexBags.listener.BackpackListeners;
import com.axther.vexBags.storage.BackpackStorage;
import com.axther.vexBags.tier.BackpackTier;
import com.axther.vexBags.util.ItemUtil;
import com.axther.vexBags.command.AdminBackpackCommand;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

public final class VexBags extends JavaPlugin {

	private static VexBags instance;
	private NamespacedKey keyBackpackId;
	private NamespacedKey keyBackpackTier;
	private NamespacedKey keyEntryIndex;
	private NamespacedKey keyBackpackSig;
	private NamespacedKey keyBackpackVersion;
	private NamespacedKey keyBackpackSession;
	private String serverSecret;

	@Override
	public void onEnable() {
		instance = this;
		this.keyBackpackId = new NamespacedKey(this, "backpack_id");
		this.keyBackpackTier = new NamespacedKey(this, "backpack_tier");
		this.keyEntryIndex = new NamespacedKey(this, "entry_index");
		this.keyBackpackSig = new NamespacedKey(this, "backpack_sig");
		this.keyBackpackVersion = new NamespacedKey(this, "backpack_ver");
		this.keyBackpackSession = new NamespacedKey(this, "backpack_sess");

		// Load secret (dupe protection signature)
		saveDefaultConfig();
		this.serverSecret = getConfig().getString("secret");
		if (this.serverSecret == null || this.serverSecret.isEmpty()) {
			this.serverSecret = java.util.UUID.randomUUID().toString().replace("-", "");
			getConfig().set("secret", this.serverSecret);
			saveConfig();
		}

		// Ensure data folder exists and load storage
		BackpackStorage.get().init(this);

		// Register commands
		if (getCommand("vexbags") != null) {
			var cmd = getCommand("vexbags");
			cmd.setExecutor(new VexBagsCommand());
			cmd.setTabCompleter(new VexBagsCommand());
		}
		if (getCommand("vexbagsadmin") != null) {
			var acmd = getCommand("vexbagsadmin");
			AdminBackpackCommand admin = new AdminBackpackCommand();
			acmd.setExecutor(admin);
			acmd.setTabCompleter(admin);
		}

		// Register listeners
		Bukkit.getPluginManager().registerEvents(new BackpackListeners(), this);

		// Register crafting recipes for each tier
		registerBackpackRecipes();
	}

	@Override
	public void onDisable() {
		// Persist any pending changes
		BackpackStorage.get().saveNow();
	}

	public static VexBags getInstance() {
		return instance;
	}

	public NamespacedKey getKeyBackpackId() {
		return keyBackpackId;
	}

	public NamespacedKey getKeyBackpackTier() {
		return keyBackpackTier;
	}

	public NamespacedKey getKeyEntryIndex() { return keyEntryIndex; }

	public NamespacedKey getKeyBackpackSig() { return keyBackpackSig; }

	public NamespacedKey getKeyBackpackVersion() { return keyBackpackVersion; }

	public NamespacedKey getKeyBackpackSession() { return keyBackpackSession; }

	public String getServerSecret() { return serverSecret; }

	private void registerBackpackRecipes() {
		// Clear old recipes this plugin may have registered previously (helpful during /reload in dev)
		Iterator<Recipe> iterator = Bukkit.recipeIterator();
		while (iterator.hasNext()) {
			Recipe recipe = iterator.next();
			if (recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equals(getName().toLowerCase())) {
				iterator.remove();
			}
		}

		for (BackpackTier tier : BackpackTier.values()) {
			ItemUtil.registerRecipeForTier(this, tier);
		}
	}
}

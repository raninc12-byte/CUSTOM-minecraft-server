package com.gulis.skyblock.core;

import com.gulis.skyblock.admin.AdminCommand;
import com.gulis.skyblock.admin.AdminGUI;
import com.gulis.skyblock.admin.PlayerManagerGUI;
import com.gulis.skyblock.admin.ServerControlGUI;
import com.gulis.skyblock.economy.EconomyCommand;
import com.gulis.skyblock.economy.EconomyManager;
import com.gulis.skyblock.gui.GUIListener;
import com.gulis.skyblock.gui.GUIManager;
import com.gulis.skyblock.island.IslandCommand;
import com.gulis.skyblock.island.IslandManager;
import com.gulis.skyblock.shop.ShopCommand;
import com.gulis.skyblock.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for the custom Skyblock plugin.
 *
 * <p>Wires together all subsystems: configuration, database, economy, shop,
 * islands, GUI framework and the admin panel. All managers are exposed via
 * getters so other modules can reach shared state without static singletons.</p>
 */
public class Skyblock extends JavaPlugin {

    private static Skyblock instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private ShopManager shopManager;
    private IslandManager islandManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        // Ensure data folder exists before loading configs/db
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create plugin data folder! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // --- Core managers ---
        this.configManager = new ConfigManager(this);
        configManager.loadAll();

        this.databaseManager = new DatabaseManager(this);
        databaseManager.connect();
        databaseManager.createTables();

        this.economyManager = new EconomyManager(this);
        this.shopManager = new ShopManager(this);
        shopManager.load();

        this.islandManager = new IslandManager(this);
        islandManager.loadIslands();

        // --- GUI framework ---
        this.guiManager = new GUIManager();
        Bukkit.getPluginManager().registerEvents(new GUIListener(guiManager), this);

        // --- Commands ---
        getCommand("island").setExecutor(new IslandCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("admin").setExecutor(new AdminCommand(this));
        getCommand("balance").setExecutor(new EconomyCommand(this));
        getCommand("pay").setExecutor(new EconomyCommand(this));

        // Buy/Sell/SellAll are handled by ShopCommand too (shared logic)
        ShopCommand shopCommand = new ShopCommand(this);
        getCommand("buy").setExecutor(shopCommand);
        getCommand("sell").setExecutor(shopCommand);
        getCommand("sellall").setExecutor(shopCommand);

        // Note: Admin/Shop GUIs do not need to be registered as listeners.
        // The GUIListener routes clicks via the InventoryHolder mechanism,
        // so any GUI that implements GUI (which sets itself as the holder)
        // is handled automatically.

        getLogger().info("Skyblock plugin enabled successfully.");
    }

    @Override
    public void onDisable() {
        // Persist any in-memory state
        if (islandManager != null) {
            islandManager.saveIslands();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Skyblock plugin disabled.");
    }

    public static Skyblock getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }
}

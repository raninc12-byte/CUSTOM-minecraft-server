package com.gulis.skyblock.menu;

import com.gulis.skyblock.admin.AdminGUI;
import com.gulis.skyblock.core.Skyblock;
import com.gulis.skyblock.gui.GUI;
import com.gulis.skyblock.island.Island;
import com.gulis.skyblock.shop.ShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The main menu shown to players when they join the server.
 *
 * <p>Layout (27-slot chest):</p>
 * <pre>
 * Row 1: [Skyblock] [OneBlock] [Shop]
 * Row 2: [My Island] [Balance] [Admin*]
 * Row 3: [Close]
 * </pre>
 *
 * <p>The {@code Admin} button (slot 16) is only visible and clickable by the
 * configured owner username (default {@code gladgulis1972}). For everyone else
 * the slot shows a "no access" barrier instead.</p>
 */
public class MainMenuGUI extends GUI {

    private final Skyblock plugin;

    // Slot constants
    private static final int SLOT_SKYBLOCK = 10;
    private static final int SLOT_ONEBLOCK = 11;
    private static final int SLOT_SHOP = 12;
    private static final int SLOT_MY_ISLAND = 13;
    private static final int SLOT_BALANCE = 14;
    private static final int SLOT_ADMIN = 16;
    private static final int SLOT_CLOSE = 22;

    public MainMenuGUI(Skyblock plugin, Player player) {
        super(player, "&8&lMain Menu", 27);
        this.plugin = plugin;
    }

    /**
     * Returns the username allowed to use the admin button. Read from
     * config.yml so it can be changed without recompiling.
     */
    private String getOwnerUsername() {
        return plugin.getConfigManager().getConfig()
                .getString("admin.owner-username", "gladgulis1972");
    }

    @Override
    public void build() {
        // --- Game modes ---
        inventory.setItem(SLOT_SKYBLOCK, createItem(new ItemStack(Material.GRASS_BLOCK),
                "&a&lSkyblock",
                "&7Classic skyblock on your own island.",
                "&7Click to go to your island.",
                "",
                "&ePlayers online: &f" + plugin.getServer().getOnlinePlayers().size()));

        inventory.setItem(SLOT_ONEBLOCK, createItem(new ItemStack(Material.BEDROCK),
                "&9&lOneBlock",
                "&7Start on a single block and",
                "&7mine it to progress through phases.",
                "",
                "&7Click to start OneBlock."));

        // --- Utilities ---
        inventory.setItem(SLOT_SHOP, createItem(new ItemStack(Material.EMERALD),
                "&2&lShop",
                "&7Buy and sell items.",
                "&7Click to open the shop GUI."));

        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        String islandStatus = island == null
                ? "&cYou do not have an island yet."
                : "&aLevel: &f" + island.getLevel();
        inventory.setItem(SLOT_MY_ISLAND, createItem(new ItemStack(Material.OAK_SAPLING),
                "&6&lMy Island",
                islandStatus,
                "&7Click to teleport home."));

        double balance = plugin.getEconomyManager().getBalance(player);
        inventory.setItem(SLOT_BALANCE, createItem(new ItemStack(Material.GOLD_INGOT),
                "&e&lBalance",
                "&7Your balance: &f" + plugin.getEconomyManager().format(balance),
                "&7Click to refresh."));

        // --- Admin button: only the configured owner can see/use it ---
        String owner = getOwnerUsername();
        if (owner.equalsIgnoreCase(player.getName())) {
            inventory.setItem(SLOT_ADMIN, createItem(new ItemStack(Material.REDSTONE),
                    "&4&lAdmin Panel",
                    "&cRestricted to: &f" + owner,
                    "&7Click to open the admin panel."));
        } else {
            inventory.setItem(SLOT_ADMIN, createItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE),
                    "&7&lLocked",
                    "&cAdmin access only.",
                    "&7Restricted to: &f" + owner));
        }

        // --- Close ---
        inventory.setItem(SLOT_CLOSE, createItem(new ItemStack(Material.BARRIER),
                "&cClose", "&7Click to close this menu."));

        // Fill remaining slots with glass panes for a clean look
        ItemStack filler = createItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "&7");
        for (int i = 0; i < 27; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (slot) {
            case SLOT_SKYBLOCK:
                player.closeInventory();
                player.performCommand("is home");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);
                return;
            case SLOT_ONEBLOCK:
                player.closeInventory();
                player.performCommand("oneblock");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);
                return;
            case SLOT_SHOP:
                ShopGUI shop = new ShopGUI(plugin, player);
                shop.open();
                plugin.getGuiManager().register(player, shop);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                return;
            case SLOT_MY_ISLAND:
                player.closeInventory();
                player.performCommand("is home");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                return;
            case SLOT_BALANCE:
                refresh();
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                return;
            case SLOT_ADMIN:
                if (!getOwnerUsername().equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.RED + "You do not have access to the admin panel.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                    return;
                }
                AdminGUI admin = new AdminGUI(plugin, player);
                admin.open();
                plugin.getGuiManager().register(player, admin);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                return;
            case SLOT_CLOSE:
                player.closeInventory();
                return;
            default:
                break;
        }
    }
}

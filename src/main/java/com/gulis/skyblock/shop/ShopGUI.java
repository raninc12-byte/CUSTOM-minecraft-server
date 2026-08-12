package com.gulis.skyblock.shop;

import com.gulis.skyblock.core.Skyblock;
import com.gulis.skyblock.gui.GUI;
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
 * The main shop GUI with category selection and paginated item browsing.
 *
 * <p>Layout (54-slot chest):</p>
 * <pre>
 * Row 1-2: Category icons (clicking opens that category's item list)
 * Row 3-5: Items in the selected category (paginated, 21 per page)
 * Row 6:   [← Prev] [Page info] [Next →] [Close]
 * </pre>
 *
 * <p>Click behavior:</p>
 * <ul>
 *   <li>Left-click an item → buy 1</li>
 *   <li>Right-click an item → sell 1</li>
 *   <li>Shift-left-click → buy 64 (a stack)</li>
 *   <li>Shift-right-click → sell all of that item from inventory</li>
 * </ul>
 */
public class ShopGUI extends GUI {

    private final Skyblock plugin;
    private String currentCategory;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 21;
    private static final int ITEMS_START_SLOT = 11; // row 2 col 2 (after category row)

    public ShopGUI(Skyblock plugin, Player player) {
        super(player, "&8&lSkyblock Shop", 54);
        this.plugin = plugin;
    }

    @Override
    public void build() {
        if (currentCategory == null) {
            buildCategoryMenu();
        } else {
            buildItemMenu();
        }
    }

    /**
     * Builds the top-level category selection view.
     */
    private void buildCategoryMenu() {
        // Filler border
        ItemStack filler = createItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "&7");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
        }

        int slot = 9;
        for (ShopManager.ShopCategory category : plugin.getShopManager().getCategories().values()) {
            if (slot >= 18) break; // only 9 category slots in row 2
            ItemStack icon = createItem(new ItemStack(category.getIcon()),
                    category.getDisplayName(),
                    "&7Click to browse items.");
            inventory.setItem(slot, icon);
            slot++;
        }

        // Bottom row: close button
        ItemStack close = createItem(new ItemStack(Material.BARRIER), "&cClose", "&7Click to close.");
        inventory.setItem(49, close);
    }

    /**
     * Builds the paginated item list for the currently selected category.
     */
    private void buildItemMenu() {
        ShopManager.ShopCategory category = plugin.getShopManager().getCategory(currentCategory);
        if (category == null) {
            currentCategory = null;
            buildCategoryMenu();
            return;
        }

        // Top row: back button + category title
        ItemStack back = createItem(new ItemStack(Material.ARROW), "&a← Back", "&7Return to categories.");
        inventory.setItem(0, back);

        ItemStack filler = createItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "&7");
        for (int i = 1; i < 9; i++) {
            inventory.setItem(i, filler);
        }

        List<ShopManager.ShopItem> items = category.getItems();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        int slot = ITEMS_START_SLOT;
        for (int i = startIndex; i < endIndex; i++) {
            ShopManager.ShopItem shopItem = items.get(i);
            ItemStack display = buildShopItemIcon(shopItem);
            // Place items in a 7-wide grid (slots 11-17, 20-26, 35-41)
            inventory.setItem(slot, display);
            slot++;
            // Skip the border columns (slot 8 and 9 of each row section)
            if ((slot - ITEMS_START_SLOT) % 7 == 0) {
                slot += 2; // skip the two border slots
            }
        }

        // Bottom row: pagination + close
        int totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        if (page > 0) {
            ItemStack prev = createItem(new ItemStack(Material.ARROW), "&a← Previous Page",
                    "&7Go to page " + page);
            inventory.setItem(45, prev);
        }
        ItemStack pageInfo = createItem(new ItemStack(Material.PAPER), "&fPage " + (page + 1) + "/" + Math.max(1, totalPages));
        inventory.setItem(49, pageInfo);
        if (page < totalPages - 1) {
            ItemStack next = createItem(new ItemStack(Material.ARROW), "&aNext Page →",
                    "&7Go to page " + (page + 2));
            inventory.setItem(53, next);
        }

        ItemStack close = createItem(new ItemStack(Material.BARRIER), "&cClose", "&7Click to close.");
        inventory.setItem(48, close);
    }

    /**
     * Builds the display icon for a shop item, showing buy/sell prices in lore.
     */
    private ItemStack buildShopItemIcon(ShopManager.ShopItem shopItem) {
        ItemStack item = new ItemStack(shopItem.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', shopItem.getDisplayName()));
            List<String> lore = new ArrayList<>();
            String buyStr = shopItem.getBuyPrice() > 0
                    ? ChatColor.GREEN + "Buy: " + ChatColor.GOLD + plugin.getEconomyManager().format(shopItem.getBuyPrice())
                    : ChatColor.RED + "Not buyable";
            String sellStr = shopItem.getSellPrice() > 0
                    ? ChatColor.GREEN + "Sell: " + ChatColor.GOLD + plugin.getEconomyManager().format(shopItem.getSellPrice())
                    : ChatColor.RED + "Not sellable";
            lore.add(buyStr);
            lore.add(sellStr);
            lore.add("");
            lore.add(ChatColor.GRAY + "Left-click: Buy 1");
            lore.add(ChatColor.GRAY + "Right-click: Sell 1");
            lore.add(ChatColor.GRAY + "Shift-Left: Buy 64");
            lore.add(ChatColor.GRAY + "Shift-Right: Sell all");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        event.setCancelled(true);

        // Top-level category view
        if (currentCategory == null) {
            if (slot == 49) { // close
                player.closeInventory();
                return;
            }
            // Category icons occupy slots 9-17
            if (slot >= 9 && slot < 18) {
                int index = slot - 9;
                List<ShopManager.ShopCategory> cats = new ArrayList<>(
                        plugin.getShopManager().getCategories().values());
                if (index < cats.size()) {
                    currentCategory = cats.get(index).getId();
                    page = 0;
                    refresh();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
            }
            return;
        }

        // Item menu view
        switch (slot) {
            case 0: // back
                currentCategory = null;
                page = 0;
                refresh();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                return;
            case 45: // prev page
                if (page > 0) {
                    page--;
                    refresh();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
                return;
            case 53: // next page
                ShopManager.ShopCategory cat = plugin.getShopManager().getCategory(currentCategory);
                int totalPages = (int) Math.ceil((double) cat.getItems().size() / ITEMS_PER_PAGE);
                if (page < totalPages - 1) {
                    page++;
                    refresh();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
                return;
            case 48: // close
                player.closeInventory();
                return;
            default:
                break;
        }

        // Item click: buy or sell
        ShopManager.ShopItem shopItem = findShopItem(slot);
        if (shopItem == null) return;

        boolean shift = event.isShiftClick();
        boolean right = event.isRightClick();

        if (right && !shift) {
            sellItem(shopItem, 1);
        } else if (right && shift) {
            sellAllOfType(shopItem);
        } else if (!right && shift) {
            buyItem(shopItem, 64);
        } else {
            buyItem(shopItem, 1);
        }
    }

    /**
     * Maps an inventory slot back to the ShopItem it represents.
     */
    private ShopManager.ShopItem findShopItem(int slot) {
        ShopManager.ShopCategory category = plugin.getShopManager().getCategory(currentCategory);
        if (category == null) return null;
        // Items start at slot 11, arranged in a 7-wide grid with 2-slot gaps per row
        int relative = slot - ITEMS_START_SLOT;
        if (relative < 0) return null;
        int row = relative / 9; // each visual row is 9 slots
        int col = relative % 9;
        if (col >= 7) return null; // border columns
        int index = row * 7 + col;
        List<ShopManager.ShopItem> items = category.getItems();
        int absoluteIndex = page * ITEMS_PER_PAGE + index;
        if (absoluteIndex < 0 || absoluteIndex >= items.size()) return null;
        return items.get(absoluteIndex);
    }

    /**
     * Buys {@code amount} of an item. Checks balance, withdraws money, and
     * gives the item to the player (dropping overflow on the ground).
     */
    private void buyItem(ShopManager.ShopItem shopItem, int amount) {
        if (shopItem.getBuyPrice() <= 0) {
            player.sendMessage(ChatColor.RED + "This item cannot be bought.");
            return;
        }
        double total = shopItem.getBuyPrice() * amount;
        if (!plugin.getEconomyManager().has(player, total)) {
            player.sendMessage(ChatColor.RED + "You cannot afford that. Need "
                    + plugin.getEconomyManager().format(total) + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }
        // Simulate adding the item to check for inventory space
        ItemStack stack = new ItemStack(shopItem.getMaterial(), amount);
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        if (!overflow.isEmpty()) {
            int notAdded = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
            int actual = amount - notAdded;
            if (actual <= 0) {
                player.sendMessage(ChatColor.RED + "Your inventory is full!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                return;
            }
            // Adjust price to what actually fit
            total = shopItem.getBuyPrice() * actual;
            amount = actual;
        }
        plugin.getEconomyManager().withdraw(player, total);
        player.sendMessage(ChatColor.GREEN + "Bought " + amount + "x "
                + shopItem.getDisplayName() + ChatColor.GREEN + " for "
                + ChatColor.GOLD + plugin.getEconomyManager().format(total) + ChatColor.GREEN + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.2f);
        refresh();
    }

    /**
     * Sells {@code amount} of an item from the player's inventory.
     */
    private void sellItem(ShopManager.ShopItem shopItem, int amount) {
        if (shopItem.getSellPrice() <= 0) {
            player.sendMessage(ChatColor.RED + "This item cannot be sold.");
            return;
        }
        int available = countItem(shopItem.getMaterial());
        if (available <= 0) {
            player.sendMessage(ChatColor.RED + "You do not have any of that item.");
            return;
        }
        int toSell = Math.min(amount, available);
        removeItem(shopItem.getMaterial(), toSell);
        double total = shopItem.getSellPrice() * toSell;
        plugin.getEconomyManager().deposit(player, total);
        player.sendMessage(ChatColor.GREEN + "Sold " + toSell + "x "
                + shopItem.getDisplayName() + ChatColor.GREEN + " for "
                + ChatColor.GOLD + plugin.getEconomyManager().format(total) + ChatColor.GREEN + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        refresh();
    }

    /**
     * Sells every unit of the clicked item type in the player's inventory.
     */
    private void sellAllOfType(ShopManager.ShopItem shopItem) {
        if (shopItem.getSellPrice() <= 0) {
            player.sendMessage(ChatColor.RED + "This item cannot be sold.");
            return;
        }
        int available = countItem(shopItem.getMaterial());
        if (available <= 0) {
            player.sendMessage(ChatColor.RED + "You do not have any of that item.");
            return;
        }
        removeItem(shopItem.getMaterial(), available);
        double total = shopItem.getSellPrice() * available;
        plugin.getEconomyManager().deposit(player, total);
        player.sendMessage(ChatColor.GREEN + "Sold " + available + "x "
                + shopItem.getDisplayName() + ChatColor.GREEN + " for "
                + ChatColor.GOLD + plugin.getEconomyManager().format(total) + ChatColor.GREEN + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        refresh();
    }

    private int countItem(Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItem(Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
    }
}

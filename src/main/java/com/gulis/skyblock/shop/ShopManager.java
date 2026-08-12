package com.gulis.skyblock.shop;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads shop categories and item prices from {@code shops.yml}.
 *
 * <p>The config structure is:</p>
 * <pre>
 * categories:
 *   blocks:
 *     icon: STONE
 *     name: "&aBuilding Blocks"
 *     items:
 *       STONE:
 *         buy: 10.0
 *         sell: 2.0
 *         name: "&7Stone"
 * </pre>
 *
 * <p>Each category is parsed into a {@link ShopCategory} and cached. Item
 * prices are also indexed by {@link Material} for fast lookup from the
 * {@code /buy} and {@code /sell} commands.</p>
 */
public class ShopManager {

    private final Skyblock plugin;
    private final Map<String, ShopCategory> categories = new HashMap<>();
    private final Map<Material, ShopItem> itemIndex = new HashMap<>();

    public ShopManager(Skyblock plugin) {
        this.plugin = plugin;
    }

    /**
     * (Re)loads all shop categories from {@code shops.yml}.
     */
    public void load() {
        categories.clear();
        itemIndex.clear();

        ConfigurationSection root = plugin.getConfigManager().get("shops.yml")
                .getConfigurationSection("categories");
        if (root == null) {
            plugin.getLogger().warning("No 'categories' section found in shops.yml!");
            return;
        }

        for (String categoryId : root.getKeys(false)) {
            ConfigurationSection catSection = root.getConfigurationSection(categoryId);
            if (catSection == null) continue;

            String iconMatName = catSection.getString("icon", "CHEST");
            Material icon = matchMaterial(iconMatName, categoryId);
            String displayName = catSection.getString("name", "&f" + categoryId);

            ShopCategory category = new ShopCategory(categoryId, displayName, icon);
            ConfigurationSection itemsSection = catSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String materialName : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(materialName);
                    if (itemSection == null) continue;

                    Material material = matchMaterial(materialName, categoryId);
                    if (material == null || material == Material.AIR) continue;

                    double buy = itemSection.getDouble("buy", -1.0);   // -1 = not buyable
                    double sell = itemSection.getDouble("sell", -1.0);  // -1 = not sellable
                    String itemName = itemSection.getString("name", "&f" + materialName);

                    ShopItem shopItem = new ShopItem(material, buy, sell, itemName);
                    category.addItem(shopItem);
                    itemIndex.put(material, shopItem);
                }
            }
            categories.put(categoryId, category);
        }

        plugin.getLogger().info("Loaded " + categories.size() + " shop categories, "
                + itemIndex.size() + " items.");
    }

    private Material matchMaterial(String name, String categoryId) {
        Material material = Material.matchMaterial(name);
        if (material == null) {
            plugin.getLogger().warning("Unknown material '" + name + "' in shop category '" + categoryId + "' — skipped.");
        }
        return material;
    }

    public ShopCategory getCategory(String id) {
        return categories.get(id);
    }

    public Map<String, ShopCategory> getCategories() {
        return categories;
    }

    public ShopItem getItem(Material material) {
        return itemIndex.get(material);
    }

    public double getBuyPrice(Material material) {
        ShopItem item = itemIndex.get(material);
        return item == null ? -1.0 : item.getBuyPrice();
    }

    public double getSellPrice(Material material) {
        ShopItem item = itemIndex.get(material);
        return item == null ? -1.0 : item.getSellPrice();
    }

    public boolean isBuyable(Material material) {
        return getBuyPrice(material) > 0;
    }

    public boolean isSellable(Material material) {
        return getSellPrice(material) > 0;
    }

    /**
     * A shop category: an icon, a display name, and a list of items.
     */
    public static class ShopCategory {
        private final String id;
        private final String displayName;
        private final Material icon;
        private final List<ShopItem> items = new ArrayList<>();

        public ShopCategory(String id, String displayName, Material icon) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
        }

        public void addItem(ShopItem item) {
            items.add(item);
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        public List<ShopItem> getItems() { return items; }
    }

    /**
     * A single shop item with buy/sell prices. A price of -1 means the item
     * cannot be bought (or sold) through the shop.
     */
    public static class ShopItem {
        private final Material material;
        private final double buyPrice;
        private final double sellPrice;
        private final String displayName;

        public ShopItem(Material material, double buyPrice, double sellPrice, String displayName) {
            this.material = material;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.displayName = displayName;
        }

        public Material getMaterial() { return material; }
        public double getBuyPrice() { return buyPrice; }
        public double getSellPrice() { return sellPrice; }
        public String getDisplayName() { return displayName; }
    }
}

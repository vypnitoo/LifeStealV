package com.vypnito.lifestealV;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.Arrays;

public class HeartManager {

    public static final NamespacedKey HEART_ITEM_KEY = new NamespacedKey("lifestealv", "heart_item");
    public static final NamespacedKey REVIVE_ITEM_KEY = new NamespacedKey("lifestealv", "revive_item");

    public static ItemStack createHeartItem() {
        ItemStack heartItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = heartItem.getItemMeta();
        meta.setDisplayName("§cHeart");
        meta.setLore(Arrays.asList("§7A fragment of a life.", "§cRight-click to consume and gain one heart."));
        meta.getPersistentDataContainer().set(HEART_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        heartItem.setItemMeta(meta);
        return heartItem;
    }

    public static boolean isHeartItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(HEART_ITEM_KEY, PersistentDataType.BYTE);
    }

    public static ItemStack createReviveItem() {
        ItemStack reviveItem = new ItemStack(Material.BEACON);
        ItemMeta meta = reviveItem.getItemMeta();
        meta.setDisplayName("§bHeart of Revival");
        meta.setLore(Arrays.asList("§7Powerful enough to restore a lost soul.", "§bHold this and use /revive <player>."));
        meta.getPersistentDataContainer().set(REVIVE_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        reviveItem.setItemMeta(meta);
        return reviveItem;
    }

    public static boolean isReviveItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(REVIVE_ITEM_KEY, PersistentDataType.BYTE);
    }
}
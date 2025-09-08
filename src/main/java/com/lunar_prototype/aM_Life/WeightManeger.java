package com.lunar_prototype.aM_Life;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class WeightManeger {

    private final JavaPlugin plugin;
    private final NamespacedKey weightKey;

    public WeightManeger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.weightKey = new NamespacedKey("custom", "weight"); // custom:weight 相当
    }

    public double getPlayerWeight(Player player) {
        double totalWeight = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            if (meta.getPersistentDataContainer().has(weightKey, PersistentDataType.INTEGER)) {
                double weight = Double.valueOf(String.valueOf(meta.getPersistentDataContainer().get(weightKey, PersistentDataType.INTEGER)));
                totalWeight += weight * item.getAmount(); // 複数個対応
            }
        }

        return totalWeight;
    }
}
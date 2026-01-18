package com.lunar_prototype.aM_Life;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;

public class BackpackManager {

    private final AM_Life plugin;
    private final File dataFolder;

    public BackpackManager(AM_Life plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "backpack_data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * バックパックのアイテムデータをロードする。
     * @param uuid バックパックのUUID
     * @param size バックパックのサイズ (スロット数)
     * @return ロードされたItemStack[]。データがない場合は空の配列。
     */
    public ItemStack[] load(String uuid, int size) {
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) {
            return new ItemStack[size]; // 新しいバックパック
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ItemStack[] contents = new ItemStack[size];

        for (int i = 0; i < size; i++) {
            // YamlConfigurationはItemStackを直接ロードできる
            contents[i] = config.getItemStack("contents." + i);
        }

        return contents;
    }

    /**
     * バックパックのアイテムデータを保存する。
     * @param uuid バックパックのUUID
     * @param contents 保存するItemStack[]
     */
    public void save(String uuid, ItemStack[] contents) {
        File file = new File(dataFolder, uuid + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                // YamlConfigurationはItemStackを直接保存できる
                config.set("contents." + i, contents[i]);
            } else {
                config.set("contents." + i, null); // nullを明示的にセット
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save backpack data for UUID: " + uuid);
            e.printStackTrace();
        }
    }
}
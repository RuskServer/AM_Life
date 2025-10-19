package com.lunar_prototype.aM_Life;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class hideout {
    private FileConfiguration hideoutData;

    public void loadHideoutData() {
        File file = new File(AM_Life.getInstance().getDataFolder(), "hideouts.yml");
        if (!file.exists()) AM_Life.getInstance().saveResource("hideouts.yml", false);
        hideoutData = YamlConfiguration.loadConfiguration(file);
    }

    public void saveHideoutData() {
        try {
            hideoutData.save(new File(AM_Life.getInstance().getDataFolder(), "hideouts.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getHideoutData() {
        return hideoutData;
    }
}

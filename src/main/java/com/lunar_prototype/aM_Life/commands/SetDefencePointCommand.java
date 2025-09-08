package com.lunar_prototype.aM_Life.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SetDefencePointCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public SetDefencePointCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cプレイヤーのみ使用できます。");
            return true;
        }

        Location loc = player.getLocation();
        File file = new File(plugin.getDataFolder(), "defencepoints.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<String> list = config.getStringList(loc.getWorld().getName());
        String serialized = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        list.add(serialized);
        config.set(loc.getWorld().getName(), list);

        try {
            config.save(file);
            player.sendMessage("§a防衛地点を追加しました: " + serialized);
        } catch (IOException e) {
            player.sendMessage("§c保存に失敗しました。");
            e.printStackTrace();
        }
        return true;
    }
}

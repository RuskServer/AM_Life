package com.lunar_prototype.aM_Life.commands;

import com.lunar_prototype.aM_Life.AM_Life;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

public class HideoutCommand implements CommandExecutor {
    private final AM_Life plugin;

    public HideoutCommand(AM_Life plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        UUID uuid = player.getUniqueId();

        FileConfiguration data = plugin.getHideout().getHideoutData();

        // 既に登録済み？
        if (data.contains("players." + uuid)) {
            String worldName = data.getString("players." + uuid + ".world");
            double x = data.getDouble("players." + uuid + ".x");
            double y = data.getDouble("players." + uuid + ".y");
            double z = data.getDouble("players." + uuid + ".z");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage("§cエラー: Hideoutのワールドが見つかりません。");
                return true;
            }

            // 📍相対位置を適用（x+2, y+3, z-6）
            Location teleportLoc = new Location(world, x + 2, y + 3, z - 6);

            player.teleport(teleportLoc);
            player.sendMessage("§aあなたのHideoutへ移動しました。");
            return true;
        }

        // 未作成 → 生成
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Location spawnLoc = generateHideoutInstance(player);
                Bukkit.getScheduler().runTask(plugin, () -> player.teleport(spawnLoc));
                player.sendMessage("§a新しいHideoutが作成されました！");
            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage("§cHideoutの生成に失敗しました。");
            }
        });

        return true;
    }

    private Location generateHideoutInstance(Player player) throws Exception {
        World world = Bukkit.getWorld("hideout_world");
        if (world == null) throw new Exception("hideout_worldが存在しません。");

        // 📍 プレイヤーごとの生成位置を自動オフセット
        FileConfiguration data = plugin.getHideout().getHideoutData();
        int count = data.getConfigurationSection("players") != null
                ? data.getConfigurationSection("players").getKeys(false).size()
                : 0;

        int offsetX = count * 100; // 200ブロック間隔で並べる
        Location pasteLoc = new Location(world, offsetX, 64, 0);

        // ✅ WorldEditでschematicを貼り付け
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            ClipboardFormat format = ClipboardFormats.findByFile(new File(plugin.getDataFolder(), "hideout.schem"));
            try (ClipboardReader reader = format.getReader(new FileInputStream(new File(plugin.getDataFolder(), "hideout.schem")))) {
                Clipboard clipboard = reader.read();
                clipboard.paste(editSession, BlockVector3.at(pasteLoc.getX(), pasteLoc.getY(), pasteLoc.getZ()), false);
            }
        }

        // 永続化
        UUID uuid = player.getUniqueId();
        data.set("players." + uuid + ".world", "hideout_world");
        data.set("players." + uuid + ".x", pasteLoc.getX());
        data.set("players." + uuid + ".y", pasteLoc.getY());
        data.set("players." + uuid + ".z", pasteLoc.getZ());
        plugin.getHideout().saveHideoutData();

        return pasteLoc.clone().add(0, 1, 0);
    }
}


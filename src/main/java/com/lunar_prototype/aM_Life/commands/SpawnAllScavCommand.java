package com.lunar_prototype.aM_Life.commands;

import com.lunar_prototype.aM_Life.ScavEntity;
import com.lunar_prototype.aM_Life.ScavManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SpawnAllScavCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ScavManager scavManager;

    public SpawnAllScavCommand(JavaPlugin plugin,ScavManager scavManager) {
        this.plugin = plugin;
        this.scavManager = scavManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§c使い方: /spawnallscav <ワールド名> <数>");
            return true;
        }

        String worldName = args[0];
        int total;
        try {
            total = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数値が不正です。");
            return true;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage("§c指定されたワールドが存在しません。");
            return true;
        }

        int patrolCount = total / 2;
        int defenceCount = total - patrolCount;

        // 巡回役をスポーン
        for (int i = 0; i < patrolCount; i++) {
            Location spawnLoc = getRandomPatrolSpawn(world); // 既存の patrolPoints からランダム取得
            Chunk chunk = spawnLoc.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load(true);
            }
            scavManager.spawnSCAV(spawnLoc,null);
            // 巡回モード（defencelocなし）
        }

        // 防衛役をスポーン（最大3体/拠点）
        List<Location> defencePoints = loadDefencePoints(world);
        Map<Location, Integer> assigned = new HashMap<>();
        Random rand = new Random();

        for (int i = 0; i < defenceCount; i++) {
            if (defencePoints.isEmpty()) break;

            Location targetPoint;
            if (defenceCount <= defencePoints.size()) {
                // 少数なら集中（最初の拠点）
                targetPoint = defencePoints.get(0);
            } else {
                // ランダム割り当て、1拠点3体まで
                int attempts = 0;
                do {
                    targetPoint = defencePoints.get(rand.nextInt(defencePoints.size()));
                    attempts++;
                } while (assigned.getOrDefault(targetPoint, 0) >= 3 && attempts < 10);

                // 10回試してもダメなら最初に戻る
                if (assigned.getOrDefault(targetPoint, 0) >= 3) {
                    continue;
                }
            }

            Chunk chunk = targetPoint.getChunk();
            if (!chunk.isLoaded()) chunk.load(true);

            assigned.put(targetPoint, assigned.getOrDefault(targetPoint, 0) + 1);
            Bukkit.getLogger().info("Spawning SCAV at " + targetPoint + " [defender=" + (targetPoint != null) + "]");
            scavManager.spawnSCAV(targetPoint, targetPoint);
        }

        sender.sendMessage("§aScavをスポーンしました（巡回: " + patrolCount + " / 防衛: " + defenceCount + "）");
        return true;
    }

    //ランダムなPatrolPointを算出
    private Location getRandomPatrolSpawn(World world) {
        List<Location> patrolPoints = new ArrayList<>();

        File file = new File(plugin.getDataFolder(), "patrols/" + world.getName() + ".yml");
        if (!file.exists()) return world.getSpawnLocation(); // fallback

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> pointStrings = config.getStringList("patrolPoints");
        for (String s : pointStrings) {
            String[] parts = s.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            patrolPoints.add(new Location(world, x + 0.5, y, z + 0.5));
        }

        if (patrolPoints.isEmpty()) return world.getSpawnLocation(); // fallback

        return patrolPoints.get(new Random().nextInt(patrolPoints.size()));
    }
    // 防衛地点を読み込み
    public List<Location> loadDefencePoints(World world) {
        File file = new File(plugin.getDataFolder(), "defencepoints.yml");
        if (!file.exists()) return new ArrayList<>();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> list = config.getStringList(world.getName());
        List<Location> locations = new ArrayList<>();

        for (String s : list) {
            String[] parts = s.split(",");
            if (parts.length != 3) continue;
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            locations.add(new Location(world, x + 0.5, y, z + 0.5));
        }
        return locations;
    }
}


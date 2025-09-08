package com.lunar_prototype.aM_Life.commands;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionOwner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class SetMapping implements CommandExecutor {
    private final JavaPlugin plugin;

    public SetMapping(JavaPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)){
            sender.sendMessage("このコマンドはプレイヤーのみ使用可能です");
            return true;
        }
        mapAndSavePoints(((Player) sender).getPlayer());
        return true;
    }
    public void mapAndSavePoints(Player player) {
        try {
            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
            Region region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            World world = player.getWorld();

            List<Location> patrolPoints = new ArrayList<>();
            Random random = new Random();

            // 巡回地点として許可する床ブロック
            Set<Material> allowedFloorBlocks = Set.of(Material.GRASS_BLOCK, Material.STONE, Material.COBBLESTONE);

            for (int x = region.getMinimumPoint().getBlockX(); x <= region.getMaximumPoint().getBlockX(); x += 5) {
                for (int z = region.getMinimumPoint().getBlockZ(); z <= region.getMaximumPoint().getBlockZ(); z += 5) {
                    if (random.nextDouble() < 0.5) continue;

                    int y = getSafeGroundY(world, x, z);
                    if (y == -1) continue;

                    Location loc = new Location(world, x + 0.5, y, z + 0.5);
                    Material floor = world.getBlockAt(x, y - 1, z).getType();

                    if (!allowedFloorBlocks.contains(floor)) continue;
                    if (!isValidPatrolLocation(loc)) continue; // ★ここで移動可能かチェック

                    patrolPoints.add(loc);
                }
            }

            File file = new File(plugin.getDataFolder(), "patrols/" + world.getName() + ".yml");
            file.getParentFile().mkdirs();

            YamlConfiguration config = new YamlConfiguration();
            config.set("patrolPoints", patrolPoints.stream()
                    .map(loc -> loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ())
                    .collect(Collectors.toList()));
            config.save(file);

            player.sendMessage("§a" + patrolPoints.size() + " 箇所をマッピングしました！");
        } catch (Exception e) {
            player.sendMessage("§c範囲が設定されていません！");
            e.printStackTrace();
        }
    }

    private int getSafeGroundY(World world, int x, int z) {
        for (int y = world.getMaxHeight() - 1; y > world.getMinHeight(); y--) {
            Material below = world.getBlockAt(x, y - 1, z).getType();
            Material body = world.getBlockAt(x, y, z).getType();
            Material head = world.getBlockAt(x, y + 1, z).getType();

            if (below.isSolid() && body.isAir() && head.isAir()) {
                return y;
            }
        }
        return -1; // 有効な地面が見つからなかった
    }

    private boolean isValidPatrolLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        Location below = loc.clone().add(0, -1, 0);
        Location body = loc.clone();
        Location head = loc.clone().add(0, 1, 0);

        return below.getBlock().getType().isSolid()
                && body.getBlock().isPassable()
                && head.getBlock().isPassable();
    }

}

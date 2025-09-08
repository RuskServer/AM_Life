package com.lunar_prototype.aM_Life;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.bukkit.*;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.Pillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ScavManager {

    private final Map<UUID, ScavEntity> scavMap = new HashMap<>();

    private final JavaPlugin plugin;

    public ScavManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void spawnSCAV(Location spawn,Location defenceloc) {
        World world = spawn.getWorld();

        Pillager scav = world.spawn(spawn, Pillager.class);
        scav.setAI(true); // AI有効化
        scav.setCustomNameVisible(false); //カスタムネームタグ無効化
        scav.setRemoveWhenFarAway(false);
        scav.setPersistent(true);
        NamespacedKey scavKey = new NamespacedKey(plugin, "isScav");
        scav.getPersistentDataContainer().set(scavKey, PersistentDataType.BYTE, (byte) 1);
        Mob nmsMob = ((CraftMob) scav).getHandle();
        GoalSelector goalSelector = nmsMob.goalSelector;
        GoalSelector targetSelector = nmsMob.targetSelector;

        goalSelector.getAvailableGoals().clear();
        targetSelector.getAvailableGoals().clear();

        // 例: プレイヤー風に偽装（名前は"SCAV"）
        Disguise disguise = DisguiseAPI.getCustomDisguise("SCAV");

        // 適用
        DisguiseAPI.disguiseToAll(scav, disguise);

        List<String> weapons = Arrays.asList("AK_47", "SKS", "Uzi", "Glock_17");
        String randomWeapon = weapons.get(new Random().nextInt(weapons.size()));

        if (defenceloc != null) {
            ScavEntity scavEntity = new ScavEntity(scav,plugin,world,randomWeapon,defenceloc);
            scavMap.put(scav.getUniqueId(), scavEntity);
            return;
        }
        ScavEntity scavEntity = new ScavEntity(scav,plugin,world,randomWeapon,null);
        scavMap.put(scav.getUniqueId(), scavEntity);
    }

    public void startScavController() {
        new BukkitRunnable() {
            @Override
            public void run() {
                scavMap.values().removeIf(ScavEntity::isDead);
                for (ScavEntity scavEntity : scavMap.values()) {
                    scavEntity.start();
                }
            }
        }.runTaskTimer(AM_Life.getInstance(), 0L, 10L); // 10tickごと
    }
    public void startScavChunkLoad() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ScavEntity scav : scavMap.values()) {
                    if (scav.isDead()) continue;
                    Location loc = scav.getEntity().getLocation();
                    Chunk chunk = loc.getChunk();
                    if (!chunk.isLoaded()) {
                        chunk.load(true);
                    }
                    chunk.addPluginChunkTicket(plugin); // ← これを毎tickやると完璧
                }
            }
        }.runTaskTimer(AM_Life.getInstance(), 0L, 1L);
    }
    public Collection<ScavEntity> getActiveScavs() {
        return scavMap.values();
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Pillager pillager)) return;

        NamespacedKey scavKey = new NamespacedKey(plugin, "isScav");
        if (pillager.getPersistentDataContainer().has(scavKey, PersistentDataType.BYTE)) {
            List<String> weapons = Arrays.asList("AK_47", "SKS", "Uzi", "Glock_17");
            String randomWeapon = weapons.get(new Random().nextInt(weapons.size()));
            Disguise disguise = DisguiseAPI.getCustomDisguise("SCAV");
            DisguiseAPI.disguiseToAll(pillager, disguise);
            ScavEntity scav = new ScavEntity(pillager, plugin, pillager.getWorld(), randomWeapon,null);
            scavMap.put(pillager.getUniqueId(), scav);
        }
    }
}


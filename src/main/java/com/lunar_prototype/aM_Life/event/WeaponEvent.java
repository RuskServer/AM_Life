package com.lunar_prototype.aM_Life.event;

import com.lunar_prototype.aM_Life.ScavEntity;
import com.lunar_prototype.aM_Life.ScavManager;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.weaponevents.ProjectileExplodeEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponEquipEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponShootEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.List;

public class WeaponEvent implements Listener {
    private final ScavManager scavManager;

    public WeaponEvent(ScavManager scavManager) {
        this.scavManager = scavManager;
    }
    @EventHandler
    public void onWeaponShoot(WeaponShootEvent event) {
        if (event.getShooter() instanceof Player) {
            Player shooter = (Player) event.getShooter();
            Location shootLoc = shooter.getLocation();

            for (ScavEntity scav : scavManager.getActiveScavs()) {
                if (!scav.getEntity().getWorld().equals(shootLoc.getWorld())) continue;

                double distance = scav.getEntity().getLocation().distance(shootLoc);
                if (distance > 50) continue;

                // 遮蔽物数を数えて音の減衰を計算
                int blockCount = countSolidBlocksBetween(scav.getEntity().getEyeLocation(), shootLoc);
                double obstructionFactor = Math.max(0.1, 1.0 - (blockCount * 0.02)); // 1ブロックあたり5%減衰
                double probability = obstructionFactor * (1.0 - (distance / 50.0)); // 遠いほど小さく

                if (Math.random() < probability) {
                    scav.setCurrentLocationTarget(shootLoc); // SCAVに向かわせる処理
                }
            }
        }
    }

    @EventHandler
    public void WeaponEquipEvent(WeaponEquipEvent event) {
        if (event.getShooter() instanceof Player) {
            Player shooter = (Player) event.getShooter();
            int cooldown = WeaponMechanics.getConfigurations().getInt(event.getWeaponTitle() + ".Info.Weapon_Equip_Delay") / 40;
            shooter.setCooldown(event.getWeaponStack(), cooldown);

        }
    }
    @EventHandler
    public void ProjectileExplodeEvent(ProjectileExplodeEvent event){
        List<Block> blocks = event.getBlocks();
        for (Block block : blocks) {
            Location loc = block.getLocation().add(0.5, 0.5, 0.5); // ブロックの中心
            World world = block.getWorld();
            world.spawnParticle(Particle.EXPLOSION, loc, 1); // 爆発パーティクルを1つ表示
        }
    }

    public int countSolidBlocksBetween(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);
        int blocks = 0;

        for (double i = 0; i < distance; i += 1.0) {
            Location point = from.clone().add(direction.clone().multiply(i));
            Block block = point.getBlock();
            if (block.getType().isSolid()) {
                blocks++;
            }
        }

        return blocks;
    }
}

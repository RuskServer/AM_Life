package com.lunar_prototype.aM_Life;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class StatusEffectManager {

    private final Map<UUID, Set<StatusEffect>> activeEffects = new HashMap<>();
    private final Map<UUID, BukkitTask> bleedingTasks = new HashMap<>();

    public enum StatusEffect {
        LIGHT_BLEEDING, // 軽出血
        HEAVY_BLEEDING, // 重出血
        FRACTURE       // 骨折
    }

    // 状態異常を付与するメソッド
    public void addEffect(Player player, StatusEffect effect) {
        UUID uuid = player.getUniqueId();
        activeEffects.computeIfAbsent(uuid, k -> EnumSet.noneOf(StatusEffect.class)).add(effect);
    }

    // 状態異常を解除するメソッド
    public void removeEffect(Player player, StatusEffect effect) {
        UUID uuid = player.getUniqueId();
        Set<StatusEffect> effects = activeEffects.get(uuid);
        if (effects != null) {
            effects.remove(effect);
            if (effects.isEmpty()) {
                activeEffects.remove(uuid);
            }
        }
    }

    // プレイヤーが特定の状態異常にかかっているかチェック
    public boolean hasEffect(Player player, StatusEffect effect) {
        Set<StatusEffect> effects = activeEffects.get(player.getUniqueId());
        return effects != null && effects.contains(effect);
    }

    // プレイヤーが持っているすべての状態異常を取得
    public Set<StatusEffect> getActiveEffects(Player player) {
        return activeEffects.getOrDefault(player.getUniqueId(), EnumSet.noneOf(StatusEffect.class));
    }

    public Map<UUID, BukkitTask> getBleedingTasks() {
        return bleedingTasks;
    }
}
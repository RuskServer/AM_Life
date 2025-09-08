package com.lunar_prototype.aM_Life;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Set;

public class StaminaPlaceholder extends PlaceholderExpansion {

    private final StaminaListener staminaListener;
    private final StatusEffectManager statusEffectManager;

    public StaminaPlaceholder(StaminaListener listener,StatusEffectManager statusEffectManager) {
        this.staminaListener = listener;
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public String getIdentifier() {
        return "amlife"; // %amlife_stamina% のように使われる識別子
    }

    @Override
    public String getAuthor() {
        return "LunarPrototype";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean canRegister() {
        return true; // この拡張機能を登録できるか
    }

    @Override
    public boolean persist() {
        return true; // プラグインのリロード時に再登録不要
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        if (params.equals("stamina")) {
            // StaminaListenerからスタミナ値を取得して返す
            int stamina = staminaListener.getPlayerStamina(player.getUniqueId());
            return String.valueOf(stamina);
        }
        if (params.equals("weight")) {
            WeightManeger weightManeger = new WeightManeger(AM_Life.getInstance());
            double weight = weightManeger.getPlayerWeight(player.getPlayer());
            return String.valueOf(weight);
        }
        if (params.equals("effect")) {
            return formatEffects(player.getPlayer());
        }

        // プレースホルダーが見つからない場合
        return null;
    }

    private String formatEffects(Player player) {
        Set<StatusEffectManager.StatusEffect> effects = statusEffectManager.getActiveEffects(player);
        if (effects.isEmpty()) {
            return "なし";
        }

        StringBuilder sb = new StringBuilder();
        for (StatusEffectManager.StatusEffect effect : effects) {
            switch (effect) {
                case FRACTURE:
                    sb.append("§c骨折§r ");
                    break;
                case LIGHT_BLEEDING:
                    sb.append("§e軽出血§r ");
                    break;
                case HEAVY_BLEEDING:
                    sb.append("§4重出血§r ");
                    break;
            }
        }
        return sb.toString().trim();
    }
}
package com.lunar_prototype.aM_Life;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaminaListener implements Listener {

    // 最大スタミナ値
    private static final int MAX_STAMINA = 400;
    private static final double WEIGHT_LIMIT = 50.0;
    private static final long REGEN_DELAY_MS = 1000L; // 1秒の遅延

    // プレイヤーごとのスタミナ値を管理するマップ
    private final Map<UUID, Integer> playerStamina = new HashMap<>();
    // 最後に動いた時間を記録するマップ
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();

    public StaminaListener() {
        startStaminaTask();
        // PlaceholderAPIの統合は、メインクラス（AM_Life）で行う
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // プレイヤーが参加したらスタミナを最大値に設定
        playerStamina.put(uuid, MAX_STAMINA);
        updateSpeedLater(player);
    }

    public int getPlayerStamina(UUID uuid) {
        return playerStamina.getOrDefault(uuid, MAX_STAMINA);
    }

    // 満腹度をスタミナとして使わないため、onFoodChange イベントは削除

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        WeightManeger weightManeger = new WeightManeger(AM_Life.getInstance());
        double weight = weightManeger.getPlayerWeight(player);
        int currentStamina = playerStamina.getOrDefault(uuid, MAX_STAMINA);

        boolean moved = event.getFrom().distanceSquared(event.getTo()) > 0.0001;

        // ダッシュ速度の調整（重すぎなら最低速度）
        float speed;
        if (weight > WEIGHT_LIMIT) {
            speed = 0.05f;
        } else {
            speed = player.isSprinting() ? 0.3f : (float) Math.max(0.05f, 0.2f - (weight / 300f));
        }
        player.setWalkSpeed(speed);

        // スタミナ消費とlastMoveTimeの更新
        if (player.isSprinting()) {
            if (currentStamina > 0) {
                // スタミナを1減らす
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
                playerStamina.put(uuid, currentStamina - 1);
            } else {
                // スタミナが0なら走れないようにする
                player.setSprinting(false);
            }
            // 走っている時のみ、最後に動いた時間を更新
            if (moved) {
                lastMoveTime.put(uuid, System.currentTimeMillis());
            }
        }
    }

    private void startStaminaTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : AM_Life.getInstance().getServer().getOnlinePlayers()) {
                    if (player.isDead()) continue;

                    UUID uuid = player.getUniqueId();
                    int currentStamina = playerStamina.getOrDefault(uuid, MAX_STAMINA);

                    // スタミナが最大値なら回復しない
                    if (currentStamina >= MAX_STAMINA) {
                        continue;
                    }

                    // 走っている場合はスタミナを回復させない
                    if (player.isSprinting()) {
                        continue;
                    }

                    // 最後に動いた時間を確認
                    long lastMoved = lastMoveTime.getOrDefault(uuid, System.currentTimeMillis());
                    long now = System.currentTimeMillis();

                    // 1秒の遅延後、回復を開始
                    if (now - lastMoved < REGEN_DELAY_MS) {
                        continue;
                    }

                    // 回復量
                    int regenAmount = 0;
                    double velocitySquared = player.getVelocity().lengthSquared();
                    boolean isStill = velocitySquared < 0.001; // ほぼ停止しているか

                    if (isStill) {
                        // 完全に停止している時
                        regenAmount = 4;
                    } else {
                        // 歩いている時
                        regenAmount = 2;
                    }

                    if (regenAmount > 0) {
                        int newStamina = Math.min(MAX_STAMINA, currentStamina + regenAmount);
                        playerStamina.put(uuid, newStamina);
                    }
                }
            }
        }.runTaskTimer(AM_Life.getInstance(), 0L, 10L); // 0.5秒ごとに実行
    }

    // 他のイベントハンドラーは元のコードのままでOK
    @EventHandler
    public void onInventoryChange(PlayerItemHeldEvent event) {
        updateSpeedLater(event.getPlayer());
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        updateSpeedLater(event.getPlayer());
    }

    private void updateSpeedLater(Player player) {
        new BukkitRunnable() {
            public void run() {
                WeightManeger weightManeger = new WeightManeger(AM_Life.getInstance());
                double weight = weightManeger.getPlayerWeight(player);
                float speed = (float) Math.max(0.05f, 0.2f - (weight / 300f));
                if (weight > WEIGHT_LIMIT) {
                    player.setWalkSpeed(0.05f);
                } else {
                    player.setWalkSpeed(player.isSprinting() ? 0.3f : speed);
                }
            }
        }.runTaskLater(AM_Life.getInstance(), 1L);
    }
}
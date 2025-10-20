package com.lunar_prototype.aM_Life;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class StatusEffectListener implements Listener {

    private final AM_Life plugin;
    private final StatusEffectManager statusEffectManager;
    private final Random random = new Random();

    public StatusEffectListener(AM_Life plugin, StatusEffectManager statusEffectManager) {
        this.plugin = plugin;
        this.statusEffectManager = statusEffectManager;
        startFractureTask();
    }

    // ダメージイベント
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();

        // 落下ダメージによる骨折
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (event.getDamage() > 5.0) { // ダメージが5.0を超えた場合
                statusEffectManager.addEffect(player, StatusEffectManager.StatusEffect.FRACTURE);
                player.sendMessage("§c足が折れた！");
            }
        }
        // 落下ダメージ以外のダメージによる出血
        else {
            int rand = random.nextInt(100); // 0から99のランダムな整数

            if (rand < 3) { // 3%の確率
                statusEffectManager.addEffect(player, StatusEffectManager.StatusEffect.HEAVY_BLEEDING);
                startBleedingTask(player, StatusEffectManager.StatusEffect.HEAVY_BLEEDING);
                player.sendMessage("§4重出血を起こした！");
            } else if (rand < 12) { // 9%の確率 (3 + 9 = 12)
                statusEffectManager.addEffect(player, StatusEffectManager.StatusEffect.LIGHT_BLEEDING);
                startBleedingTask(player, StatusEffectManager.StatusEffect.LIGHT_BLEEDING);
                player.sendMessage("§c出血している！");
            }
        }
    }

    private void startBleedingTask(Player player, StatusEffectManager.StatusEffect effect) {
        UUID uuid = player.getUniqueId();
        // 既にタスクが実行中の場合は何もしない
        if (statusEffectManager.getBleedingTasks().containsKey(uuid)) {
            return;
        }

        long delay = 0;
        long period = 0;

        if (effect == StatusEffectManager.StatusEffect.HEAVY_BLEEDING) {
            delay = 0;
            period = 9 * 20L; // 9秒
        } else if (effect == StatusEffectManager.StatusEffect.LIGHT_BLEEDING) {
            delay = 0;
            period = 20 * 20L; // 20秒
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // プレイヤーが重出血または軽出血にかかっているかチェック
                if (statusEffectManager.hasEffect(player, StatusEffectManager.StatusEffect.HEAVY_BLEEDING)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 2, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 22, 1, true, false));
                } else if (statusEffectManager.hasEffect(player, StatusEffectManager.StatusEffect.LIGHT_BLEEDING)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1, true, false));
                } else {
                    // どちらの出血状態でもなければタスクをキャンセル
                    this.cancel();
                    statusEffectManager.getBleedingTasks().remove(uuid);
                }
            }
        }.runTaskTimer(plugin, delay, period);

        // タスクをマップに保存
        statusEffectManager.getBleedingTasks().put(uuid, task);
    }

    // 骨折の効果を定期的に付与するタスク（これは変更なしで良い）
    private void startFractureTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (statusEffectManager.hasEffect(player, StatusEffectManager.StatusEffect.FRACTURE)) {
                        player.setSprinting(false);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 22, 1, true, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    //死亡イベント
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        // 状態異常を管理するマップからプレイヤーを削除
        statusEffectManager.getActiveEffects(player).clear();

        // 出血タスクをキャンセル
        BukkitTask bleedingTask = statusEffectManager.getBleedingTasks().get(uuid);
        if (bleedingTask != null) {
            bleedingTask.cancel();
            statusEffectManager.getBleedingTasks().remove(uuid);
        }
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return;
        }

        // プレイヤーが Crouch(かがむ)状態ではないことを確認し、アイテムがカスタムアイテムかチェック
        if (!player.isSneaking() && isCustomItem(item, "AI_2")) {
            event.setCancelled(true);

            // AI-2の使用開始
            startAi2Use(player, item);
        }
        // CAT止血帯の使用ロジックをここに追加
        if (!player.isSneaking() && isCustomItem(item, "CAT")) {
            event.setCancelled(true);
            startCatUse(player, item);
        }
    }

    private void startAi2Use(Player player, ItemStack item) {
        // 既存のタスクが実行中であれば何もしない（重複防止）
        if (isUsingItem.containsKey(player.getUniqueId())) {
            return;
        }

        // 元のアイテムのカスタムモデルデータを取得
        int originalModelData = getCustomModelData(item);

        // アイテムの消費量を追跡するためにアイテムスロットを保持
        int itemSlot = player.getInventory().getHeldItemSlot();

        // 使用開始時のアニメーション
        setCustomModelData(player, item, 10);
        player.sendMessage("§bAI-2を使用中… (3秒)");

        // 使用中フラグを立てる
        isUsingItem.put(player.getUniqueId(), true);
        player.setSprinting(false);

        int useTimeTicks = 3 * 20; // 3秒をtickに変換

        new BukkitRunnable() {
            int ticksPassed = 0;

            @Override
            public void run() {
                // 中断条件のチェック
                // プレイヤーがスロットを変更したか
                ItemStack currentItem = player.getInventory().getItemInMainHand();
                if (player.isDead() || !player.isOnline() || player.getInventory().getHeldItemSlot() != itemSlot) {

                    // 元のカスタムモデルデータに戻す
                    player.setSprinting(true);
                    setCustomModelData(player, currentItem, originalModelData);
                    player.sendMessage("§cAI-2の使用をキャンセルしました。");
                    isUsingItem.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                if (ticksPassed >= useTimeTicks) {
                    // 使用完了
                    double maxHealth = player.getMaxHealth();
                    double healAmount = maxHealth * 0.3;
                    double newHealth = Math.min(maxHealth, player.getHealth() + healAmount);

                    player.setHealth(newHealth);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
                    player.sendMessage("§aAI-2を使用し、ヘルスが回復した。");
                    player.setSprinting(true);

                    // アイテムを消費
                    currentItem.setAmount(currentItem.getAmount() - 1);

                    // 元のカスタムモデルデータに戻す
                    setCustomModelData(player, currentItem, originalModelData);
                    isUsingItem.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                ticksPassed++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // 1 tickごとに実行
    }

    /**
     * CAT止血帯の使用を開始するメソッド
     */
    private void startCatUse(Player player, ItemStack item) {
        // 既存のタスクが実行中であれば何もしない（重複防止）
        if (isUsingItem.containsKey(player.getUniqueId())) {
            return;
        }

        // 使用中フラグを立てる
        isUsingItem.put(player.getUniqueId(), true);

        player.sendMessage("§6CAT止血帯を使用中… (3秒)");

        int useTimeTicks = 3 * 20; // 3秒をtickに変換
        int itemSlot = player.getInventory().getHeldItemSlot();

        new BukkitRunnable() {
            int ticksPassed = 0;

            @Override
            public void run() {
                // 中断条件のチェック
                if (player.isDead() || !player.isOnline() || player.getInventory().getHeldItemSlot() != itemSlot) {
                    player.sendMessage("§cCAT止血帯の使用をキャンセルしました。");
                    isUsingItem.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                if (ticksPassed >= useTimeTicks) {
                    // 使用完了

                    // 重出血を治療
                    statusEffectManager.removeEffect(player, StatusEffectManager.StatusEffect.HEAVY_BLEEDING);
                    statusEffectManager.removeEffect(player, StatusEffectManager.StatusEffect.LIGHT_BLEEDING);
                    player.sendMessage("§a出血が止まった！");

                    // 効果音とメッセージ
                    player.playSound(player.getLocation(), Sound.BLOCK_WOOL_BREAK, 1.0f, 1.0f);

                    // アイテムを消費
                    item.setAmount(item.getAmount() - 1);
                    isUsingItem.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                ticksPassed++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // 1 tickごとに実行
    }

    /**
     * 指定されたIDのカスタムアイテムかどうかを判定するヘルパーメソッド。
     * PersistentDataContainerを使用してIDをチェックします。
     */
    private boolean isCustomItem(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        NamespacedKey key = new NamespacedKey("customitemsplugin", "custom_item_id");
        PersistentDataContainer data = meta.getPersistentDataContainer();

        return data.has(key, PersistentDataType.STRING) && data.get(key, PersistentDataType.STRING).equals(id);
    }

    // プレイヤーが使用中か管理するマップ
    private final Map<UUID, Boolean> isUsingItem = new HashMap<>();

    // ヘルパーメソッド: アイテムのカスタムモデルデータを変更
    private void setCustomModelData(Player player, ItemStack item, int modelData) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(modelData);
            item.setItemMeta(meta);
            // インベントリの更新
            player.getInventory().setItemInMainHand(item);
        }
    }

    // ヘルパーメソッド: アイテムのカスタムモデルデータを取得
    private int getCustomModelData(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return (meta != null && meta.hasCustomModelData()) ? meta.getCustomModelData() : 0;
    }
}
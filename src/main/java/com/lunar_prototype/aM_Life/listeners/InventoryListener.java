package com.lunar_prototype.aM_Life.listeners;

import com.lunar_prototype.aM_Life.AM_Life;
import com.lunar_prototype.aM_Life.BackpackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class InventoryListener implements Listener {

    private final AM_Life plugin;
    private final BackpackManager manager;
    private final NamespacedKey uuidKey;
    private final NamespacedKey levelKey;

    public InventoryListener(AM_Life plugin,BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.uuidKey = new NamespacedKey("custom", AM_Life.KEY_BACKPACK_UUID);
        this.levelKey = new NamespacedKey("custom", AM_Life.KEY_BACKPACK_LEVEL);
    }

    // ホットバーを除くインベントリスロット (9-35)
    private static final int INVENTORY_START = 9;
    private static final int INVENTORY_END = 35;

    /**
     * プレイヤーのインベントリの状態を、チェストリグの装備レベルに基づいて更新します。
     */
    public void updatePlayerInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack leggings = inv.getLeggings();

        AM_Life.getInstance().getLogger().info("" + leggings);

        // PDCからリグのレベルを取得
        int rigLevel = AM_Life.getRigLevel(leggings);

        // レベルに応じて解放する最大インデックスを計算 (Level 1: 9-17, Level 2: 9-26, Level 3: 9-35)
        // Level 1 -> 9*1 + 8 = 17
        // Level 2 -> 9*2 + 8 = 26
        // Level 3 -> 9*3 + 8 = 35
        int unlockedEnd = (rigLevel * 9) + 8;

        // インベントリの全27スロットをチェック
        for (int i = INVENTORY_START; i <= INVENTORY_END; i++) {
            ItemStack currentItem = inv.getItem(i);

            if (i <= unlockedEnd) {
                // --- スロットが解放されている場合 ---

                // ロッカー板ガラスがあれば削除
                if (currentItem != null && currentItem.isSimilar(AM_Life.getLockerPane())) {
                    inv.setItem(i, null);
                }
            } else {
                // --- スロットがロックされている場合 ---
                ItemStack fillItem = AM_Life.getLockerPane();

                // アイテムが既に入っている場合、ドロップさせてロックする
                if (currentItem != null && !currentItem.isSimilar(fillItem)) {
                    player.sendMessage(ChatColor.RED + "チェストリグを外したため、アイテムをドロップしました！");
                    player.getWorld().dropItemNaturally(player.getLocation(), currentItem);
                }
                inv.setItem(i, fillItem);
            }
        }
    }

    // ... onPlayerJoin, onPlayerRespawn は前回と同じ ...
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerInventory(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerInventory(event.getPlayer()), 1L);
    }
    // ...

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // 1. ロックされたスロット（板ガラス）の操作を禁止
        if (event.getSlot() >= INVENTORY_START && event.getSlot() <= INVENTORY_END) {
            ItemStack clickedItem = event.getCurrentItem();

            // ロッカー板ガラスがクリックされた場合、操作をキャンセル
            if (clickedItem != null && clickedItem.isSimilar(AM_Life.getLockerPane())) {
                event.setCancelled(true);
                return;
            }

            // 現在のレベルでロックされているスロットへのアイテム移動を禁止
            int rigLevel = AM_Life.getRigLevel(player.getEquipment().getLeggings());
            int unlockedEnd = (rigLevel * 9) + 8;

            if (event.getSlot() > unlockedEnd) {
                // ロックされたスロットへのすべてのアイテム移動を禁止
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                        event.getAction() == InventoryAction.PLACE_ALL ||
                        event.getAction() == InventoryAction.PLACE_ONE ||
                        event.getAction() == InventoryAction.PLACE_SOME) {

                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 2. レギンススロットの操作を監視し、インベントリを更新
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerInventory(player), 1L);
        }
    }

    @EventHandler
    public void onInventoryClick2(InventoryClickEvent event) {
        // サバイバルインベントリビューであることを確認
        if (event.getView().getType() != InventoryType.CRAFTING) return;

        // クリックされたスロットがクラフト結果のスロット（インデックス 0）であることを確認
        if (event.getRawSlot() <= 5 && event.getSlotType() == InventoryType.SlotType.RESULT) {

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            PlayerInventory inv = player.getInventory();

            // 1. オフハンドのバックパックをチェック
            ItemStack backpackItem = inv.getItemInOffHand();
            if (!AM_Life.isBackpack(backpackItem)) {
                return; // オフハンドにバックパックがない
            }

            ItemMeta meta = backpackItem.getItemMeta();
            String uuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
            int level = meta.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 3);
            int size = level * 9;

            // 4. GUIの生成とロード
            ItemStack[] contents = manager.load(uuid, size);

            Inventory backpackGUI = Bukkit.createInventory(player, size, "§6§lバックパック §7(UUID: " + uuid.substring(0, 8) + ")");
            backpackGUI.setContents(contents);

            // プレイヤーにGUIを表示
            player.openInventory(backpackGUI);

            // クリックをキャンセルして、通常のクラフト動作を防ぐことが多いです
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory closedInv = event.getInventory();

        // GUIのタイトルからUUID情報が含まれているか（＝バックパックGUIであるか）をチェック
        // **より強固なチェック方法**としては、InventoryHolderを使う方法があるが、ここでは簡易化のためタイトルで判断
        String title = closedInv.getViewers().get(0).getOpenInventory().getTitle();
        if (!title.startsWith("§6§lバックパック §7(UUID:")) {
            return; // バックパックGUIではない
        }

        // UUIDをタイトルから抽出 (例: "§6§lバックパック §7(UUID: xxxxxxxx)"からxxxxxxxxを取得)
        String uuidPrefix = title.substring(title.indexOf("UUID:") + 6, title.indexOf(")"));

        // **注意**: 厳密には、このタイトルからUUIDのプレフィックスしか取得できないため、
        // 永続化キーとしてUUID全体を使う場合は、**開いたUUID全体を一時的に保持する**仕組みが必要です。
        // ここでは、オフハンドのアイテムから完全なUUIDを再取得する処理に置き換えます。

        // 閉じたプレイヤーのオフハンドにあるアイテムからUUID全体を再取得する
        // (InventoryCloseEventでUUID全体を直接取得するのが難しいため、これが最も確実な方法)
        if (event.getPlayer().getInventory().getItemInOffHand().hasItemMeta()) {
            String fullUUID = event.getPlayer().getInventory().getItemInOffHand().getItemMeta().getPersistentDataContainer()
                    .get(uuidKey, PersistentDataType.STRING);

            if (fullUUID != null) {
                // データの保存
                manager.save(fullUUID, closedInv.getContents());
            }
        }
    }
}

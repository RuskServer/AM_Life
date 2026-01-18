package com.lunar_prototype.aM_Life;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.lunar_prototype.aM_Life.commands.*;
import com.lunar_prototype.aM_Life.ScavManager;
import com.lunar_prototype.aM_Life.event.WeaponEvent;
import com.lunar_prototype.aM_Life.listeners.InventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class AM_Life extends JavaPlugin {

    private static AM_Life instance;

    private StaminaListener staminaListener;
    private StatusEffectManager statusEffectManager;
    private BackpackManager backpackManager;
    private hideout Hideout;
    public static NamespacedKey RIG_LEVEL_KEY;
    private static ItemStack LOCKER_PANE;
    public static final String KEY_BACKPACK_UUID = "backpack_uuid";
    public static final String KEY_BACKPACK_LEVEL = "backpack_level";
    private static final NamespacedKey UUID_KEY = new NamespacedKey("custom", KEY_BACKPACK_UUID);
    private static final NamespacedKey LEVEL_KEY = new NamespacedKey("custom", KEY_BACKPACK_LEVEL);

    public static AM_Life getInstance() {
        return instance;
    }

    public hideout getHideout() {return Hideout;}

    public BackpackManager getBackpackManager() {
        return backpackManager;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("AM_Life Loaded!");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        instance = this;
        RIG_LEVEL_KEY = new NamespacedKey("custom","rig_level");
        initCustomItems();
        staminaListener = new StaminaListener();
        Hideout = new hideout();
        // バックパックマネージャを初期化（データの永続化を担う）
        this.backpackManager = new BackpackManager(this);
        SetMapping setMapping = new SetMapping(this);
        ScavManager scavManager = new ScavManager(this);
        Bukkit.getPluginManager().registerEvents(new WeaponEvent(scavManager),this);
        getServer().getPluginManager().registerEvents(staminaListener, this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this,backpackManager),this);
        this.statusEffectManager = new StatusEffectManager();
        // ここでStatusEffectListenerを登録
        getServer().getPluginManager().registerEvents(new StatusEffectListener(this, statusEffectManager), this);
        scavManager.startScavController();
        scavManager.startScavChunkLoad();
        Hideout.loadHideoutData();
        getCommand("scavspawn").setExecutor(new Scavspawn(scavManager));
        getCommand("setmappping").setExecutor(new SetMapping(this));
        getCommand("setdefencepoint").setExecutor(new SetDefencePointCommand(this));
        getCommand("spawnallscav").setExecutor(new SpawnAllScavCommand(this,scavManager));
        getCommand("hideout").setExecutor(new HideoutCommand(this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new StaminaPlaceholder(staminaListener,statusEffectManager).register();
        } else {
            getLogger().warning("PlaceholderAPIが見つかりませんでした。スタミナ表示機能は無効です。");
        }

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (!event.getPacketType().equals(PacketType.Play.Server.PLAYER_INFO)) return;

                WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(event);

                List<WrapperPlayServerPlayerInfo.PlayerData> entries = packet.getPlayerDataList();
                UUID selfUUID = event.getUser().getUUID();

                entries.removeIf(entry -> !entry.getUser().getUUID().equals(selfUUID));

                if (entries.isEmpty()) {
                    event.setCancelled(true);
                }
            }
        });
    }

    /**
     * ロッカー板ガラスを初期化します。
     */
    private void initCustomItems() {
        LOCKER_PANE = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = LOCKER_PANE.getItemMeta();
        paneMeta.setDisplayName(ChatColor.DARK_GRAY.toString() + ChatColor.BOLD + "■ ロックされています ■");
        paneMeta.setLore(Collections.singletonList(ChatColor.RED + "チェストリグを装備して解放"));
        LOCKER_PANE.setItemMeta(paneMeta);
    }

    /**
     * アイテムがチェストリグであり、そのレベルを取得します。
     * @return リグのレベル (1-3)。リグでなければ 0 を返します。
     */
    public static int getRigLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        // RIG_LEVEL_KEYでINTEGER型データが存在するかチェック
        if (data.has(RIG_LEVEL_KEY, PersistentDataType.INTEGER)) {
            return data.get(RIG_LEVEL_KEY, PersistentDataType.INTEGER);
        }
        return 0;
    }

    /**
     * ItemStackがカスタムバックパックであるかを確認する。
     */
    public static boolean isBackpack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(UUID_KEY, PersistentDataType.STRING);
    }

    public static ItemStack getLockerPane() {
        return LOCKER_PANE.clone();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Hideout.saveHideoutData();
    }
}

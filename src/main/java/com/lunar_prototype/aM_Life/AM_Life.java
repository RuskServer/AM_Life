package com.lunar_prototype.aM_Life;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.lunar_prototype.aM_Life.commands.Scavspawn;
import com.lunar_prototype.aM_Life.ScavManager;
import com.lunar_prototype.aM_Life.commands.SetDefencePointCommand;
import com.lunar_prototype.aM_Life.commands.SetMapping;
import com.lunar_prototype.aM_Life.commands.SpawnAllScavCommand;
import com.lunar_prototype.aM_Life.event.WeaponEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class AM_Life extends JavaPlugin {

    private static AM_Life instance;

    private StaminaListener staminaListener;
    private StatusEffectManager statusEffectManager;

    public static AM_Life getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("AM_Life Loaded!");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        instance = this;
        staminaListener = new StaminaListener();
        SetMapping setMapping = new SetMapping(this);
        ScavManager scavManager = new ScavManager(this);
        Bukkit.getPluginManager().registerEvents(new WeaponEvent(scavManager),this);
        getServer().getPluginManager().registerEvents(staminaListener, this);
        this.statusEffectManager = new StatusEffectManager();
        // ここでStatusEffectListenerを登録
        getServer().getPluginManager().registerEvents(new StatusEffectListener(this, statusEffectManager), this);
        scavManager.startScavController();
        scavManager.startScavChunkLoad();
        getCommand("scavspawn").setExecutor(new Scavspawn(scavManager));
        getCommand("setmappping").setExecutor(new SetMapping(this));
        getCommand("setdefencepoint").setExecutor(new SetDefencePointCommand(this));
        getCommand("spawnallscav").setExecutor(new SpawnAllScavCommand(this,scavManager));

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

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

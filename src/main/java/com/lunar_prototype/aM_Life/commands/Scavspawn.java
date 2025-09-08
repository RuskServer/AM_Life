package com.lunar_prototype.aM_Life.commands;

import com.lunar_prototype.aM_Life.ScavManager;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class Scavspawn implements CommandExecutor {
    private final ScavManager scavManager;

    public Scavspawn(ScavManager scavManager){
        this.scavManager = scavManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)){
            sender.sendMessage("このコマンドはプレイヤーのみ使用可能です");
            return true;
        }

        Location spawnLocation = player.getLocation().add(0,1,0);
        scavManager.spawnSCAV(spawnLocation,null);
        player.sendMessage("SCAVをスポーンさせました");
        return true;
    }

}

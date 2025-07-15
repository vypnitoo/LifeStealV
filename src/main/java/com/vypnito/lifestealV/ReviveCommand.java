package com.vypnito.lifestealV;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReviveCommand implements CommandExecutor {

    private final LifeStealV plugin;

    public ReviveCommand(LifeStealV plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player-only-command"));
            return true;
        }
        if (!sender.hasPermission("lifestealv.command.revive")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;
        plugin.getRevivalGuiManager().openGui(player, 1);
        return true;
    }
}

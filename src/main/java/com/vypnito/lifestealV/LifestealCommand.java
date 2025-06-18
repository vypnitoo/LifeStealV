package com.vypnito.lifestealV;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LifestealCommand implements CommandExecutor, TabCompleter {

    private final LifeStealV plugin;

    public LifestealCommand(LifeStealV plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /" + label + " <sub-command>");
            return false;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            // Check for permission
            if (!sender.hasPermission("lifestealv.command.reload")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            // Execute reload logic
            plugin.reloadPluginConfig();
            sender.sendMessage("§aLifeStealV configuration has been reloaded successfully!");
            return true;
        }

        sender.sendMessage("§cUnknown sub-command. Usage: /" + label + " <reload>");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest sub-commands
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("lifestealv.command.reload")) {
                subCommands.add("reload");
            }

            return StringUtil.copyPartialMatches(args[0], subCommands, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}

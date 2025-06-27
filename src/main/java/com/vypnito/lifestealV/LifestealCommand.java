package com.vypnito.lifestealV;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LifestealCommand implements CommandExecutor, TabCompleter {

    private final LifeStealV plugin;
    private final MessageManager msg;
    private final RecipeManager recipeManager;

    public LifestealCommand(LifeStealV plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
        this.recipeManager = plugin.getRecipeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("lifestealv.command.reload")) {
                    sender.sendMessage(msg.getMessage("no-permission"));
                    return true;
                }
                plugin.reloadPluginConfig();
                sender.sendMessage(msg.getMessage("lifesteal-reload-success"));
                return true;

            case "check":
                if (!sender.hasPermission("lifestealv.command.check")) {
                    sender.sendMessage(msg.getMessage("no-permission"));
                    return true;
                }
                handleCheckCommand(sender, args, label);
                return true;

            case "editrecipe":
                if (!sender.hasPermission("lifestealv.command.editrecipe")) {
                    sender.sendMessage(msg.getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(msg.getMessage("player-only-command"));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(msg.getMessage("gui-edit-usage"));
                    return true;
                }
                Player player = (Player) sender;
                recipeManager.openRecipeEditor(player, args[1]);
                return true;

            default:
                sendHelpMessage(sender, label);
                return true;
        }
    }

    private void handleCheckCommand(CommandSender sender, String[] args, String label) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(msg.getMessage("lifesteal-help-check", new MessageManager.Placeholder("label", label)));
                return;
            }
            Player player = (Player) sender;
            sendHeartInfo(player, player);
        } else if (args.length == 2) {
            if (!sender.hasPermission("lifestealv.command.check.others")) {
                sender.sendMessage(msg.getMessage("no-permission"));
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(msg.getMessage("player-not-found", new MessageManager.Placeholder("player", args[1])));
                return;
            }
            sendHeartInfo(sender, target);
        } else {
            sender.sendMessage(msg.getMessage("lifesteal-help-check", new MessageManager.Placeholder("label", label)));
        }
    }

    private void sendHelpMessage(CommandSender sender, String label) {
        sender.sendMessage(msg.getMessage("lifesteal-help-header"));
        if (sender.hasPermission("lifestealv.command.check")) {
            sender.sendMessage(msg.getMessage("lifesteal-help-check", new MessageManager.Placeholder("label", label)));
        }
        if (sender.hasPermission("lifestealv.command.reload")) {
            sender.sendMessage(msg.getMessage("lifesteal-help-reload", new MessageManager.Placeholder("label", label)));
        }
        if (sender.hasPermission("lifestealv.command.editrecipe")) {
            sender.sendMessage(msg.getMessage("lifesteal-help-editrecipe", new MessageManager.Placeholder("label", label)));
        }
    }

    private void sendHeartInfo(CommandSender sender, Player target) {
        double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double maxHeartsFromConfig = plugin.getConfig().getDouble("max-hearts");
        int currentHearts = (int) (maxHealth / 2);
        int maxHearts = (int) (maxHeartsFromConfig / 2);
        String messageKey = (sender == target) ? "check-hearts-self" : "check-hearts-other";
        sender.sendMessage(msg.getMessage(messageKey,
                new MessageManager.Placeholder("player", target.getName()),
                new MessageManager.Placeholder("current", String.valueOf(currentHearts)),
                new MessageManager.Placeholder("max", String.valueOf(maxHearts))
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("lifestealv.command.reload")) subCommands.add("reload");
            if (sender.hasPermission("lifestealv.command.check")) subCommands.add("check");
            if (sender.hasPermission("lifestealv.command.editrecipe")) subCommands.add("editrecipe");
            return StringUtil.copyPartialMatches(args[0], subCommands, new ArrayList<>());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("check") && sender.hasPermission("lifestealv.command.check.others")) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
            }
            if (args[0].equalsIgnoreCase("editrecipe") && sender.hasPermission("lifestealv.command.editrecipe")) {
                return StringUtil.copyPartialMatches(args[1], Arrays.asList("heart_recipe", "revive_recipe"), new ArrayList<>());
            }
        }
        return Collections.emptyList();
    }
}
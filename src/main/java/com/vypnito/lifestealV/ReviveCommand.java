package com.vypnito.lifestealV;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReviveCommand implements CommandExecutor, TabCompleter {

    private final LifeStealV plugin;

    public ReviveCommand(LifeStealV plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check (redundant but good practice)
        if (!sender.hasPermission("lifestealv.command.revive")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player reviver = (Player) sender;

        if (args.length != 1) {
            reviver.sendMessage("§cUsage: /revive <player>");
            return false;
        }

        if (!HeartManager.isReviveItem(reviver.getInventory().getItemInMainHand())) {
            reviver.sendMessage("§cYou must be holding a Heart of Revival to use this command.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            reviver.sendMessage("§cPlayer '" + args[0] + "' has never played on this server.");
            return true;
        }

        String eliminationAction = plugin.getConfig().getString("elimination-action", "SPECTATOR").toUpperCase();
        boolean isEliminated = false;

        if (eliminationAction.equals("BAN") && target.isBanned()) {
            isEliminated = true;
        } else if (eliminationAction.equals("SPECTATOR") && target.isOnline() && target.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            isEliminated = true;
        }

        if (!isEliminated) {
            reviver.sendMessage("§c" + target.getName() + " is not eliminated and cannot be revived.");
            return true;
        }

        // --- Success! Perform the revival ---
        reviver.getInventory().getItemInMainHand().setAmount(reviver.getInventory().getItemInMainHand().getAmount() - 1);
        double reviveHearts = plugin.getConfig().getDouble("revive-hearts", 10.0);

        if (eliminationAction.equals("BAN")) {
            Bukkit.getBanList(BanList.Type.NAME).pardon(target.getName());
        }

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            Objects.requireNonNull(onlineTarget.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(reviveHearts);
            onlineTarget.setHealth(reviveHearts);
            onlineTarget.setGameMode(GameMode.SURVIVAL);
            onlineTarget.sendMessage("§bYou have been revived by " + reviver.getName() + "!");
        }

        Bukkit.broadcastMessage("§b" + target.getName() + " has been brought back to life by the compassionate " + reviver.getName() + "!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // This list will hold the names of all players who can be revived.
            List<String> revivablePlayers = new ArrayList<>();
            String eliminationAction = plugin.getConfig().getString("elimination-action", "SPECTATOR").toUpperCase();

            // Find players based on the elimination method.
            if (eliminationAction.equals("BAN")) {
                // Get names of all banned players.
                revivablePlayers.addAll(Bukkit.getBannedPlayers().stream()
                        .map(OfflinePlayer::getName)
                        .filter(Objects::nonNull) // Ensure name is not null
                        .collect(Collectors.toList()));
            } else { // SPECTATOR
                // Get names of all online players in spectator mode.
                revivablePlayers.addAll(Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.getGameMode() == GameMode.SPECTATOR)
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            }

            // This will hold the final list of suggestions.
            List<String> completions = new ArrayList<>();
            // Find all names from our revivable list that start with what the user has typed.
            StringUtil.copyPartialMatches(args[0], revivablePlayers, completions);
            Collections.sort(completions); // Sort the list alphabetically.
            return completions;
        }
        // Return an empty list for any other arguments.
        return Collections.emptyList();
    }
}

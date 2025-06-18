package com.vypnito.lifestealV;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class PlayerEventListener implements Listener {

    private final LifeStealV plugin;

    public PlayerEventListener(LifeStealV plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            double startHearts = plugin.getConfig().getDouble("start-hearts");
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(startHearts);
            player.setHealth(startHearts); // Also set current health
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Minecraft resets max health on death, so we need to re-apply it on respawn.
        Player player = event.getPlayer();
        double currentMax = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
        // A small delay ensures the attribute is applied correctly after respawning.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(currentMax);
        }, 1L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (HeartManager.isHeartItem(item)) {
            event.setCancelled(true); // Prevent default behavior (like placing a block)

            Player player = event.getPlayer();
            FileConfiguration config = plugin.getConfig();

            double maxHearts = config.getDouble("max-hearts");
            double heartsPerKill = config.getDouble("hearts-per-kill");
            double currentMaxHealth = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();

            if (currentMaxHealth < maxHearts) {
                double newMaxHealth = Math.min(currentMaxHealth + heartsPerKill, maxHearts);
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(newMaxHealth);
                player.sendMessage("§aYou have consumed a heart and gained more health!");
                item.setAmount(item.getAmount() - 1); // Consume one item
            } else {
                player.sendMessage("§cYou are already at the maximum number of hearts!");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        FileConfiguration config = plugin.getConfig();

        double heartsPerKill = config.getDouble("hearts-per-kill");
        double victimCurrentMaxHealth = Objects.requireNonNull(victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
        double victimNewMaxHealth = victimCurrentMaxHealth - heartsPerKill;

        // Player was killed by another player
        if (killer != null && killer != victim) {
            handlePlayerKill(killer, victim, victimNewMaxHealth, config);
        } else { // Natural death
            if (config.getBoolean("lose-heart-on-natural-death")) {
                double heartsLost = config.getDouble("hearts-lost-on-natural-death");
                victimNewMaxHealth = victimCurrentMaxHealth - heartsLost;
                handleHeartLoss(victim, victimNewMaxHealth, config);
            }
        }
    }

    private void handlePlayerKill(Player killer, Player victim, double victimNewMaxHealth, FileConfiguration config) {
        double heartsPerKill = config.getDouble("hearts-per-kill");

        // Handle victim
        handleHeartLoss(victim, victimNewMaxHealth, config);

        // Handle killer
        double killerCurrentMaxHealth = Objects.requireNonNull(killer.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
        double maxHearts = config.getDouble("max-hearts");

        if (killerCurrentMaxHealth < maxHearts) {
            double killerNewMaxHealth = Math.min(killerCurrentMaxHealth + heartsPerKill, maxHearts);
            Objects.requireNonNull(killer.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(killerNewMaxHealth);
            killer.sendMessage("§aYou have stolen a heart from " + victim.getName() + "!");
        } else {
            // Killer is at max health, give them an item instead
            if (config.getBoolean("drop-item-if-max-hearts")) {
                ItemStack heartItem = HeartManager.createHeartItem();
                // Try to add to inventory, if full, drop on the ground
                if (killer.getInventory().firstEmpty() == -1) {
                    killer.getWorld().dropItemNaturally(killer.getLocation(), heartItem);
                    killer.sendMessage("§cYou are at max health! A heart was dropped on the ground.");
                } else {
                    killer.getInventory().addItem(heartItem);
                    killer.sendMessage("§cYou are at max health! A heart has been added to your inventory.");
                }
            }
        }
    }

    private void handleHeartLoss(Player victim, double newMaxHealth, FileConfiguration config) {
        if (newMaxHealth <= 0) {
            // Player is eliminated
            Objects.requireNonNull(victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(2); // Set to 1 heart to avoid issues
            performElimination(victim, config);
        } else {
            // Player just loses a heart
            Objects.requireNonNull(victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(newMaxHealth);
            victim.sendMessage("§cYou have lost a heart!");
        }
    }

    private void performElimination(Player victim, FileConfiguration config) {
        String action = config.getString("elimination-action", "SPECTATOR").toUpperCase();
        plugin.getServer().broadcastMessage("§c" + victim.getName() + " has run out of hearts and has been eliminated!");

        // Run on the main thread to ensure API calls are safe
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            switch (action) {
                case "BAN":
                    victim.kickPlayer("You have run out of hearts and have been eliminated.");
                    Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(victim.getName(), "Eliminated from the Lifesteal SMP", null, null);
                    break;
                case "SPECTATOR":
                default:
                    victim.setGameMode(GameMode.SPECTATOR);
                    break;
            }
        });
    }
}

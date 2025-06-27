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
    private final MessageManager msg;

    public PlayerEventListener(LifeStealV plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Set initial health for first-time players
        if (!player.hasPlayedBefore()) {
            double startHearts = plugin.getConfig().getDouble("start-hearts", 20);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(startHearts);
            player.setHealth(startHearts);
        }

        // Send update notification to players with permission
        if (player.hasPermission("lifestealv.update.notify") && plugin.isUpdateAvailable()) {
            // Delay message slightly to prevent it from being lost in other join messages
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(msg.getMessage("update-available",
                        new MessageManager.Placeholder("new_version", plugin.getNewVersion()),
                        new MessageManager.Placeholder("current_version", plugin.getDescription().getVersion())
                ));
            }, 40L); // 2-second delay
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Re-apply max health on respawn, as Minecraft sometimes resets it.
        double currentMax = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(currentMax), 1L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // We only care about right-clicks
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        // Handle Heart item consumption
        if (HeartManager.isHeartItem(item)) {
            event.setCancelled(true);
            FileConfiguration config = plugin.getConfig();
            double maxHearts = config.getDouble("max-hearts");
            double currentMaxHealth = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();

            if (currentMaxHealth >= maxHearts) {
                player.sendMessage(msg.getMessage("heart-max-health"));
                return;
            }

            double heartsToGain = config.getDouble("hearts-per-kill", 2);
            double newMaxHealth = Math.min(currentMaxHealth + heartsToGain, maxHearts);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(newMaxHealth);
            player.sendMessage(msg.getMessage("heart-consumed"));
            item.setAmount(item.getAmount() - 1);
            return; // Stop processing to prevent other actions
        }

        // Handle Revive Item right-click to open the GUI
        if (HeartManager.isReviveItem(item)) {
            event.setCancelled(true);
            plugin.getRevivalGuiManager().openGui(player, 1);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        FileConfiguration config = plugin.getConfig();
        double victimCurrentMaxHealth = Objects.requireNonNull(victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
        double heartsToLose;

        if (killer != null && killer != victim) {
            heartsToLose = config.getDouble("hearts-per-kill", 2);
            handlePlayerKill(killer, victim, victimCurrentMaxHealth - heartsToLose, config);
        } else {
            if (config.getBoolean("lose-heart-on-natural-death", true)) {
                heartsToLose = config.getDouble("hearts-lost-on-natural-death", 2);
                handleHeartLoss(victim, victimCurrentMaxHealth - heartsToLose, config);
            }
        }
    }

    private void handlePlayerKill(Player killer, Player victim, double victimNewMaxHealth, FileConfiguration config) {
        handleHeartLoss(victim, victimNewMaxHealth, config);
        double killerCurrentMaxHealth = Objects.requireNonNull(killer.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
        double maxHearts = config.getDouble("max-hearts");

        if (killerCurrentMaxHealth < maxHearts) {
            double heartsToGain = config.getDouble("hearts-per-kill", 2);
            double killerNewMaxHealth = Math.min(killerCurrentMaxHealth + heartsToGain, maxHearts);
            Objects.requireNonNull(killer.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(killerNewMaxHealth);
            killer.sendMessage(msg.getMessage("heart-stolen", new MessageManager.Placeholder("player", victim.getName())));
        } else {
            if (config.getBoolean("drop-item-if-max-hearts", true)) {
                ItemStack heartItem = HeartManager.createHeartItem();
                if (killer.getInventory().firstEmpty() == -1) {
                    killer.getWorld().dropItemNaturally(killer.getLocation(), heartItem);
                } else {
                    killer.getInventory().addItem(heartItem);
                }
            }
        }
    }

    private void handleHeartLoss(Player victim, double newMaxHealth, FileConfiguration config) {
        if (newMaxHealth <= 0) {
            // Set to 1 heart before elimination to prevent issues
            Objects.requireNonNull(victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(2);
            performElimination(victim, config);
        } else {
            Objects.requireNonNull(victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(newMaxHealth);
            victim.sendMessage(msg.getMessage("heart-lost"));
        }
    }

    private void performElimination(Player victim, FileConfiguration config) {
        String action = config.getString("elimination-action", "SPECTATOR").toUpperCase();
        Bukkit.broadcastMessage(msg.getMessage("elimination-broadcast", new MessageManager.Placeholder("player", victim.getName())));
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            switch (action) {
                case "BAN":
                    victim.kickPlayer(msg.getMessage("elimination-broadcast", new MessageManager.Placeholder("player", victim.getName())));
                    Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(victim.getName(), "Eliminated from the Lifesteal SMP", null, null);
                    break;
                case "SPECTATOR":
                default:
                    victim.setGameMode(GameMode.SURVIVAL); // Spectator can be buggy, let's try survival
                    victim.setHealth(0);
                    break;
            }
        });
    }
}
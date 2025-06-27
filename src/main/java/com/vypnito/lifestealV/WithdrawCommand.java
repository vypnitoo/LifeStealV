package com.vypnito.lifestealV;

import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Objects;

public class WithdrawCommand implements CommandExecutor {

	private final LifeStealV plugin;
	private final MessageManager msg;

	public WithdrawCommand(LifeStealV plugin) {
		this.plugin = plugin;
		this.msg = plugin.getMessageManager();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(msg.getMessage("player-only-command"));
			return true;
		}
		if (!sender.hasPermission("lifestealv.command.withdraw")) {
			sender.sendMessage(msg.getMessage("no-permission"));
			return true;
		}

		Player player = (Player) sender;
		if (player.getInventory().firstEmpty() == -1) {
			player.sendMessage(msg.getMessage("withdraw-inventory-full"));
			return true;
		}

		double currentHealth = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
		double minHealth = plugin.getConfig().getDouble("minimum-health-to-withdraw", 4);
		double heartsToLose = plugin.getConfig().getDouble("hearts-per-kill", 2);

		if (currentHealth > minHealth) {
			Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(currentHealth - heartsToLose);
			ItemStack heartItem = HeartManager.createHeartItem();
			player.getInventory().addItem(heartItem);
			player.sendMessage(msg.getMessage("withdraw-success"));
		} else {
			player.sendMessage(msg.getMessage("withdraw-not-enough-hearts"));
		}
		return true;
	}
}
package com.vypnito.lifestealV;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GiveHeartCommand implements CommandExecutor, TabCompleter {

	private final LifeStealV plugin;
	private final MessageManager msg;

	public GiveHeartCommand(LifeStealV plugin) {
		this.plugin = plugin;
		this.msg = plugin.getMessageManager();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("lifestealv.command.giveheart")) {
			sender.sendMessage(msg.getMessage("no-permission"));
			return true;
		}

		if (args.length < 1) {
			sender.sendMessage(msg.getMessage("giveheart-usage"));
			return false;
		}

		Player target = Bukkit.getPlayer(args[0]);
		if (target == null) {
			sender.sendMessage(msg.getMessage("player-not-found", new MessageManager.Placeholder("player", args[0])));
			return true;
		}

		String itemType = (args.length > 1) ? args[1].toLowerCase() : "heart";
		int amount = 1;
		if (args.length > 2) {
			try {
				amount = Integer.parseInt(args[2]);
				if (amount < 1 || amount > 64) {
					sender.sendMessage(msg.getMessage("giveheart-invalid-amount"));
					return true;
				}
			} catch (NumberFormatException e) {
				sender.sendMessage(msg.getMessage("giveheart-invalid-amount"));
				return true;
			}
		}

		ItemStack itemToGive;
		if (itemType.equals("heart")) {
			itemToGive = HeartManager.createHeartItem();
		} else if (itemType.equals("revive")) {
			itemToGive = HeartManager.createReviveItem();
		} else {
			sender.sendMessage(msg.getMessage("giveheart-invalid-item"));
			return true;
		}
		itemToGive.setAmount(amount);

		if (target.getInventory().firstEmpty() == -1) {
			sender.sendMessage(msg.getMessage("giveheart-inventory-full", new MessageManager.Placeholder("player", target.getName())));
			return true;
		}

		target.getInventory().addItem(itemToGive);

		sender.sendMessage(msg.getMessage("giveheart-success-sender",
				new MessageManager.Placeholder("amount", String.valueOf(amount)),
				new MessageManager.Placeholder("item_type", itemType),
				new MessageManager.Placeholder("player", target.getName())
		));
		target.sendMessage(msg.getMessage("giveheart-success-receiver",
				new MessageManager.Placeholder("amount", String.valueOf(amount)),
				new MessageManager.Placeholder("item_type", itemType),
				new MessageManager.Placeholder("sender", sender.getName())
		));

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return StringUtil.copyPartialMatches(args[0],
					Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()),
					new ArrayList<>());
		}
		if (args.length == 2) {
			return StringUtil.copyPartialMatches(args[1], Arrays.asList("heart", "revive"), new ArrayList<>());
		}
		if (args.length == 3) {
			return StringUtil.copyPartialMatches(args[2], Arrays.asList("1", "10", "32", "64"), new ArrayList<>());
		}
		return Collections.emptyList();
	}
}
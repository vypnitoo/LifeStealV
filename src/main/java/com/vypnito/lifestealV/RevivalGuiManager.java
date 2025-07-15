package com.vypnito.lifestealV;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class RevivalGuiManager {

	private final LifeStealV plugin;
	private final MessageManager msg;
	private final Map<UUID, Integer> openPages = new HashMap<>();

	public RevivalGuiManager(LifeStealV plugin) {
		this.plugin = plugin;
		this.msg = plugin.getMessageManager();
	}

	public void openGui(Player player, int page) {
		List<OfflinePlayer> eliminated = getEliminatedPlayers();
		if (eliminated.isEmpty()) {
			player.sendMessage(msg.getMessage("revive-gui-no-one"));
			return;
		}

		int totalPages = (int) Math.ceil((double) eliminated.size() / 45.0);
		if (page < 1) page = 1;
		if (page > totalPages) page = totalPages;

		String title = msg.getMessage("revive-gui-title",
				new MessageManager.Placeholder("page", String.valueOf(page)),
				new MessageManager.Placeholder("total_pages", String.valueOf(totalPages))
		);
		Inventory gui = Bukkit.createInventory(null, 54, title);
		int startIndex = (page - 1) * 45;
		for (int i = 0; i < 45 && (startIndex + i) < eliminated.size(); i++) {
			OfflinePlayer target = eliminated.get(startIndex + i);
			ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta meta = (SkullMeta) skull.getItemMeta();

			meta.setOwningPlayer(target);
			meta.setDisplayName("§b" + target.getName());
			meta.setLore(Collections.singletonList(msg.getMessage("revive-gui-skull-lore")));
			meta.getPersistentDataContainer().set(HeartManager.REVIVE_ITEM_KEY, PersistentDataType.STRING, target.getUniqueId().toString());

			skull.setItemMeta(meta);
			gui.setItem(i, skull);
		}

		if (page > 1) {
			gui.setItem(45, createGuiItem(Material.ARROW, msg.getMessage("revive-gui-prev-page")));
		}
		if (page < totalPages) {
			gui.setItem(53, createGuiItem(Material.ARROW, msg.getMessage("revive-gui-next-page")));
		}

		player.openInventory(gui);
		openPages.put(player.getUniqueId(), page);
	}

	public void performRevival(Player reviver, String targetUuid) {
		if (!HeartManager.isReviveItem(reviver.getInventory().getItemInMainHand())) {
			reviver.sendMessage(msg.getMessage("revive-no-item"));
			return;
		}

		OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(targetUuid));
		reviver.getInventory().getItemInMainHand().setAmount(reviver.getInventory().getItemInMainHand().getAmount() - 1);

		double reviveHearts = plugin.getConfig().getDouble("revive-hearts", 10.0);
		String eliminationAction = plugin.getConfig().getString("elimination-action", "SPECTATOR").toUpperCase();

		if (eliminationAction.equals("BAN")) {
			Bukkit.getBanList(BanList.Type.NAME).pardon(target.getName());
		}

		if (target.isOnline()) {
			Player onlineTarget = target.getPlayer();
			onlineTarget.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(reviveHearts);
			onlineTarget.setHealth(reviveHearts);
			onlineTarget.setGameMode(GameMode.SURVIVAL);
		}
		Bukkit.broadcastMessage(msg.getMessage("revive-broadcast",
				new MessageManager.Placeholder("target", target.getName()),
				new MessageManager.Placeholder("reviver", reviver.getName())
		));
	}


	private List<OfflinePlayer> getEliminatedPlayers() {
		String eliminationAction = plugin.getConfig().getString("elimination-action", "SPECTATOR").toUpperCase();
		Set<OfflinePlayer> eliminated = new HashSet<>();

		if (eliminationAction.equals("BAN")) {
			eliminated.addAll(Bukkit.getBannedPlayers());
		} else { 
			eliminated.addAll(Bukkit.getOnlinePlayers().stream()
					.filter(p -> p.getGameMode() == GameMode.SPECTATOR)
					.collect(Collectors.toList()));
		}
		return new ArrayList<>(eliminated);
	}

	public Map<UUID, Integer> getOpenPages() {
		return openPages;
	}

	private ItemStack createGuiItem(Material material, String name) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(name);
			item.setItemMeta(meta);
		}
		return item;
	}
}

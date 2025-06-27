package com.vypnito.lifestealV;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GuiListener implements Listener {

	private final LifeStealV plugin;
	private final RevivalGuiManager revivalGuiManager;
	private final RecipeManager recipeManager;

	// Tuto sadu znovu přidáváme pro sledování hráčů v editoru receptů
	public static final Set<UUID> playersInEditor = new HashSet<>();
	private static final int[] CRAFTING_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};

	public GuiListener(LifeStealV plugin) {
		this.plugin = plugin;
		this.revivalGuiManager = plugin.getRevivalGuiManager();
		this.recipeManager = plugin.getRecipeManager();
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;

		String title = event.getView().getTitle();
		MessageManager msg = plugin.getMessageManager();

		// --- Handle Revival GUI ---
		String revivePrefix = msg.getMessage("revive-gui-title").split(" ")[0];
		if (title.startsWith(revivePrefix)) {
			// ... (logika pro Revival GUI zůstává stejná)
			return;
		}

		// --- Handle Recipe Editor GUI ---
		String recipePrefix = msg.getMessage("gui-recipe-editor-title").split(" ")[0];
		if (title.startsWith(recipePrefix)) {
			Player player = (Player) event.getWhoClicked();
			ItemStack clickedItem = event.getCurrentItem();

			// Allow placing items in crafting grid, but block taking items from result slot etc.
			if (event.getSlot() == 24 || (clickedItem != null && clickedItem.getType().name().endsWith("STAINED_GLASS_PANE"))) {
				event.setCancelled(true);
			}

			// Handle button clicks
			if (clickedItem != null) {
				if (clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE) { // Save button
					event.setCancelled(true);
					String recipeName = title.substring(title.indexOf(":") + 2);
					// On successful save, clear items from the grid and then close
					recipeManager.saveRecipeFromGui(player, event.getInventory(), recipeName);
					clearCraftingGrid(event.getInventory());
					player.closeInventory();
				} else if (clickedItem.getType() == Material.RED_STAINED_GLASS_PANE) { // Cancel button
					event.setCancelled(true);
					// On cancel, clear grid and return items before closing
					returnItemsAndClearGrid(player, event.getInventory());
					player.closeInventory();
				}
			}
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		UUID playerUuid = player.getUniqueId();
		Inventory inventory = event.getInventory();
		String title = event.getView().getTitle();

		// Cleanup for Revival GUI
		revivalGuiManager.getOpenPages().remove(playerUuid);

		// Cleanup for Recipe Editor GUI (if closed via Esc, etc.)
		String recipePrefix = plugin.getMessageManager().getMessage("gui-recipe-editor-title").split(" ")[0];
		if (title.startsWith(recipePrefix) && playersInEditor.contains(playerUuid)) {
			returnItemsAndClearGrid(player, inventory);
		}

		// Always remove from editor tracking on close
		playersInEditor.remove(playerUuid);
	}

	private void returnItemsAndClearGrid(Player player, Inventory inventory) {
		for (int slot : CRAFTING_SLOTS) {
			ItemStack item = inventory.getItem(slot);
			if (item != null) {
				player.getInventory().addItem(item);
			}
		}
		clearCraftingGrid(inventory);
	}

	private void clearCraftingGrid(Inventory inventory) {
		for (int slot : CRAFTING_SLOTS) {
			inventory.setItem(slot, null);
		}
	}
}
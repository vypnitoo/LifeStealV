package com.vypnito.lifestealV;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RecipeManager {

    private final LifeStealV plugin;
    private final MessageManager msg;
    private static final int[] CRAFTING_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 24;

    public RecipeManager(LifeStealV plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
    }

    public void openRecipeEditor(Player player, String recipeName) {
        ConfigurationSection recipeConfig = plugin.getRecipesConfig().getConfigurationSection("recipes." + recipeName);
        if (recipeConfig == null) {
            player.sendMessage(msg.getMessage("gui-unknown-recipe", new MessageManager.Placeholder("recipe", recipeName)));
            return;
        }

        String title = msg.getMessage("gui-recipe-editor-title", new MessageManager.Placeholder("recipe", recipeName));
        Inventory gui = Bukkit.createInventory(null, 54, title);

        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        for (int slot : CRAFTING_SLOTS) {
            gui.setItem(slot, null);
        }

        if (recipeName.equalsIgnoreCase("heart_recipe")) {
            gui.setItem(RESULT_SLOT, HeartManager.createHeartItem());
        } else if (recipeName.equalsIgnoreCase("revive_recipe")) {
            gui.setItem(RESULT_SLOT, HeartManager.createReviveItem());
        }

        gui.setItem(48, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cCancel"));
        gui.setItem(49, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aSave & Close"));

        Map<Character, ItemStack> ingredients = new HashMap<>();
        ConfigurationSection ingredientsSection = recipeConfig.getConfigurationSection("ingredients");
        if (ingredientsSection != null) {
            for (String key : ingredientsSection.getKeys(false)) {
                String materialName = ingredientsSection.getString(key);
                if (materialName == null) continue;

                ItemStack ingredientItem = materialName.equalsIgnoreCase("LIFESTEAL_HEART_ITEM")
                        ? HeartManager.createHeartItem()
                        : new ItemStack(Material.valueOf(materialName.toUpperCase()));
                ingredients.put(key.charAt(0), ingredientItem);
            }
        }

        List<String> shape = recipeConfig.getStringList("shape");
        for (int row = 0; row < shape.size(); row++) {
            String line = shape.get(row);
            for (int col = 0; col < line.length(); col++) {
                char symbol = line.charAt(col);
                if (ingredients.containsKey(symbol)) {
                    gui.setItem(CRAFTING_SLOTS[row * 3 + col], ingredients.get(symbol));
                }
            }
        }

        player.openInventory(gui);
        GuiListener.playersInEditor.add(player.getUniqueId());
    }

    public void saveRecipeFromGui(Player player, Inventory gui, String recipeName) {
        Map<Character, ItemStack> uniqueItems = new HashMap<>();
        char currentSymbol = 'A';
        List<String> shape = new ArrayList<>(Arrays.asList("   ", "   ", "   "));
        Map<String, String> newIngredientsMap = new HashMap<>();

        for (int i = 0; i < CRAFTING_SLOTS.length; i++) {
            ItemStack item = gui.getItem(CRAFTING_SLOTS[i]);
            if (item != null && item.getType() != Material.AIR) {
                char symbol = ' ';
                boolean found = false;
                for (Map.Entry<Character, ItemStack> entry : uniqueItems.entrySet()) {
                    if (entry.getValue().isSimilar(item)) {
                        symbol = entry.getKey();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    symbol = currentSymbol++;
                    uniqueItems.put(symbol, item);
                    String materialName = HeartManager.isHeartItem(item) ? "LIFESTEAL_HEART_ITEM" : item.getType().name();
                    newIngredientsMap.put(String.valueOf(symbol), materialName);
                }
                int row = i / 3;
                int col = i % 3;
                StringBuilder rowBuilder = new StringBuilder(shape.get(row));
                rowBuilder.setCharAt(col, symbol);
                shape.set(row, rowBuilder.toString());
            }
        }

        plugin.getRecipesConfig().set("recipes." + recipeName + ".shape", shape);
        plugin.getRecipesConfig().set("recipes." + recipeName + ".ingredients", newIngredientsMap);

        try {
            plugin.getRecipesConfig().save(new File(plugin.getDataFolder(), "recipes.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage("§cCould not save the recipe to file. Check console for errors.");
            return;
        }

        loadRecipes();
        player.sendMessage(msg.getMessage("gui-recipe-saved", new MessageManager.Placeholder("recipe", recipeName)));
    }

    public void loadRecipes() {
        unregisterRecipes();

        ConfigurationSection recipesSection = plugin.getRecipesConfig().getConfigurationSection("recipes");
        if (recipesSection == null) {
            plugin.getLogger().warning("No 'recipes' section found in recipes.yml.");
            return;
        }

        for (String key : recipesSection.getKeys(false)) {
            ConfigurationSection recipeConfig = recipesSection.getConfigurationSection("recipes." + key);
            if (recipeConfig == null || !recipeConfig.getBoolean("enabled", false)) {
                continue;
            }

            ItemStack result;
            if (key.equalsIgnoreCase("heart_recipe")) {
                result = HeartManager.createHeartItem();
            } else if (key.equalsIgnoreCase("revive_recipe")) {
                result = HeartManager.createReviveItem();
            } else {
                plugin.getLogger().warning("Unknown recipe key '" + key + "' in recipes.yml. Skipping.");
                continue;
            }

            ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(plugin, key), result);

            List<String> shape = recipeConfig.getStringList("shape");
            if (shape.size() != 3) continue;
            shapedRecipe.shape(shape.toArray(new String[0]));

            ConfigurationSection ingredientsSection = recipeConfig.getConfigurationSection("ingredients");
            if (ingredientsSection == null) continue;

            for (String ingredientKey : ingredientsSection.getKeys(false)) {
                char charKey = ingredientKey.charAt(0);
                String materialName = ingredientsSection.getString(ingredientKey);
                if (materialName == null) continue;

                if (materialName.equalsIgnoreCase("LIFESTEAL_HEART_ITEM")) {
                    shapedRecipe.setIngredient(charKey, new RecipeChoice.ExactChoice(HeartManager.createHeartItem()));
                } else {
                    try {
                        Material material = Material.valueOf(materialName.toUpperCase());
                        shapedRecipe.setIngredient(charKey, material);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material '" + materialName + "' in recipe '" + key + "'.");
                    }
                }
            }
            Bukkit.addRecipe(shapedRecipe);
            plugin.getLogger().info("Successfully loaded recipe: " + key);
        }
    }

    private void unregisterRecipes() {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof ShapedRecipe && ((ShapedRecipe) recipe).getKey().getNamespace().equals(plugin.getName().toLowerCase())) {
                iterator.remove();
            }
        }
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
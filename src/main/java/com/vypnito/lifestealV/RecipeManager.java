package com.vypnito.lifestealV;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;

public class RecipeManager {

    public static void registerRecipes(LifeStealV plugin) {
        // --- Heart Recipe ---
        ShapedRecipe heartRecipe = new ShapedRecipe(new NamespacedKey(plugin, "heart_recipe"), HeartManager.createHeartItem());
        heartRecipe.shape("GGG", "ONO", "DDD");
        heartRecipe.setIngredient('G', Material.GOLD_BLOCK);
        heartRecipe.setIngredient('O', Material.OBSIDIAN);
        heartRecipe.setIngredient('N', Material.NETHER_STAR);
        heartRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        Bukkit.addRecipe(heartRecipe);

        // --- Revive Recipe ---
        ShapedRecipe reviveRecipe = new ShapedRecipe(new NamespacedKey(plugin, "revive_recipe"), HeartManager.createReviveItem());
        reviveRecipe.shape(
                "DBD",
                "OHO",
                "DBD"
        );
        reviveRecipe.setIngredient('D', Material.DIAMOND);
        reviveRecipe.setIngredient('B', Material.BEACON);
        reviveRecipe.setIngredient('O', Material.OBSIDIAN);
        // This makes sure the recipe ONLY works with our custom heart item
        reviveRecipe.setIngredient('H', new RecipeChoice.ExactChoice(HeartManager.createHeartItem()));
        Bukkit.addRecipe(reviveRecipe);
    }
}

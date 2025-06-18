package com.vypnito.lifestealV;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifeStealV extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("LifeStealV has been enabled!");

        // Save the default config.yml if it doesn't exist
        saveDefaultConfig();

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);

        // Register crafting recipes
        RecipeManager.registerRecipes(this);

        // Register commands
        registerCommands();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("LifeStealV has been disabled.");
    }

    // Nová metoda pro znovunačtení konfigurace
    public void reloadPluginConfig() {
        // This is a built-in method that reloads the config.yml file from disk.
        this.reloadConfig();
    }

    // Nová metoda pro přehlednou registraci všech příkazů
    private void registerCommands() {
        // Revive Command
        ReviveCommand reviveCommand = new ReviveCommand(this);
        PluginCommand reviveCmd = this.getCommand("revive");
        if (reviveCmd != null) {
            reviveCmd.setExecutor(reviveCommand);
            reviveCmd.setTabCompleter(reviveCommand);
        }

        // Main Lifesteal Command
        LifestealCommand lifestealCommand = new LifestealCommand(this);
        PluginCommand lifestealCmd = this.getCommand("lifesteal");
        if (lifestealCmd != null) {
            lifestealCmd.setExecutor(lifestealCommand);
            lifestealCmd.setTabCompleter(lifestealCommand);
        }
    }
}

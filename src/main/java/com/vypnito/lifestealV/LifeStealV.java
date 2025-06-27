package com.vypnito.lifestealV;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LifeStealV extends JavaPlugin {

    // The SpigotMC resource ID for the update checker.
    private static final int SPIGOT_RESOURCE_ID = 126164; // <-- ID has been set!

    private MessageManager messageManager;
    private RecipeManager recipeManager;
    private RevivalGuiManager revivalGuiManager;
    private FileConfiguration recipesConfig = null;
    private File recipesFile = null;

    private boolean updateAvailable = false;
    private String newVersion = "";

    @Override
    public void onEnable() {
        getLogger().info("LifeStealV has been enabled!");
        saveDefaultConfig();

        // Initialize all managers
        messageManager = new MessageManager(this);
        messageManager.loadMessages();

        revivalGuiManager = new RevivalGuiManager(this);

        loadRecipesConfig();
        recipeManager = new RecipeManager(this);
        recipeManager.loadRecipes();

        // Register all event listeners
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        // Register all commands
        registerCommands();

        // Check for updates using the integrated ID
        if (getConfig().getBoolean("update-checker.enabled", true)) {
            checkForUpdates();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("LifeStealV has been disabled.");
    }

    public void reloadPluginConfig() {
        this.reloadConfig();
        this.messageManager.loadMessages();
        this.loadRecipesConfig();
        this.recipeManager.loadRecipes();
    }

    // --- Getters for Managers ---
    public MessageManager getMessageManager() {
        return messageManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public RevivalGuiManager getRevivalGuiManager() {
        return revivalGuiManager;
    }

    // --- Recipe Config Methods ---
    public void loadRecipesConfig() {
        if (recipesFile == null) {
            recipesFile = new File(getDataFolder(), "recipes.yml");
        }
        if (!recipesFile.exists()) {
            saveResource("recipes.yml", false);
        }
        recipesConfig = YamlConfiguration.loadConfiguration(recipesFile);
    }

    public FileConfiguration getRecipesConfig() {
        if (recipesConfig == null) {
            loadRecipesConfig();
        }
        return recipesConfig;
    }

    // --- Update Checker Methods ---
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getNewVersion() {
        return newVersion;
    }

    private void checkForUpdates() {
        if (SPIGOT_RESOURCE_ID == 0) {
            // This warning will no longer appear since the ID is now set.
            getLogger().warning("Spigot Resource ID is not set in LifeStealV.java. Disabling update checks.");
            return;
        }

        new UpdateChecker(this, SPIGOT_RESOURCE_ID).getLatestVersion(latestVersion -> {
            String currentVersion = this.getDescription().getVersion();
            if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                this.updateAvailable = true;
                this.newVersion = latestVersion;
                getLogger().info("There is a new version of LifeStealV available: " + latestVersion + " (You are using " + currentVersion + ")");
            } else {
                getLogger().info("LifeStealV is up to date!");
            }
        });
    }

    private void registerCommands() {
        // Lifesteal Command
        LifestealCommand lifestealCommand = new LifestealCommand(this);
        PluginCommand lifestealCmd = this.getCommand("lifesteal");
        if (lifestealCmd != null) {
            lifestealCmd.setExecutor(lifestealCommand);
            lifestealCmd.setTabCompleter(lifestealCommand);
        }

        // Revive Command
        this.getCommand("revive").setExecutor(new ReviveCommand(this));

        // Withdraw Command
        this.getCommand("withdraw").setExecutor(new WithdrawCommand(this));

        // GiveHeart Command
        GiveHeartCommand giveHeartCommand = new GiveHeartCommand(this);
        PluginCommand giveHeartCmd = this.getCommand("giveheart");
        if (giveHeartCmd != null) {
            giveHeartCmd.setExecutor(giveHeartCommand);
            giveHeartCmd.setTabCompleter(giveHeartCommand);
        }
    }
}
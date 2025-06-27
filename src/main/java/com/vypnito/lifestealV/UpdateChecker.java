package com.vypnito.lifestealV;

import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

	private final LifeStealV plugin;
	private final int resourceId;

	public UpdateChecker(LifeStealV plugin, int resourceId) {
		this.plugin = plugin;
		this.resourceId = resourceId;
	}

	// This method runs asynchronously to avoid lagging the server
	public void getLatestVersion(Consumer<String> consumer) {
		Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
			try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream();
				 Scanner scanner = new Scanner(inputStream)) {
				if (scanner.hasNext()) {
					final String version = scanner.next();
					Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(version));
				}
			} catch (IOException exception) {
				plugin.getLogger().info("Update checker failed: Unable to connect to SpigotMC to check for updates. (" + exception.getMessage() + ")");
			}
		});
	}
}
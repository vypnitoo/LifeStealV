package com.vypnito.lifestealV;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public class MessageManager {

	private final LifeStealV plugin;
	private FileConfiguration messagesConfig = null;
	private File messagesFile = null;

	public MessageManager(LifeStealV plugin) {
		this.plugin = plugin;
	}

	public void loadMessages() {
		if (messagesFile == null) {
			messagesFile = new File(plugin.getDataFolder(), "messages.yml");
		}
		if (!messagesFile.exists()) {
			plugin.saveResource("messages.yml", false);
		}
		messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
	}

	public String getMessage(String key) {
		String message = messagesConfig.getString(key);
		if (message == null) {
			return ChatColor.RED + "Error: Message key '" + key + "' not found in messages.yml.";
		}
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	public String getMessage(String key, Placeholder... placeholders) {
		String message = getMessage(key);
		for (Placeholder placeholder : placeholders) {
			message = message.replace(placeholder.getKey(), placeholder.getValue());
		}
		return message;
	}

	public static class Placeholder {
		private final String key;
		private final String value;

		public Placeholder(String key, String value) {
			this.key = "%" + key + "%";
			this.value = value;
		}

		public String getKey() { return key; }
		public String getValue() { return value; }
	}
}
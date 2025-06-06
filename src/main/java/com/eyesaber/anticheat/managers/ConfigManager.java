package com.eyesaber.anticheat.managers;

import com.eyesaber.anticheat.AntiCheatPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    
    private final AntiCheatPlugin plugin;
    private FileConfiguration config;
    
    public ConfigManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public boolean isCheckEnabled(String checkName) {
        return config.getBoolean("anticheat.checks." + checkName + ".enabled", true);
    }
    
    public int getCheckSensitivity(String checkName) {
        return config.getInt("anticheat.checks." + checkName + ".sensitivity", 5);
    }
    
    public int getMaxViolations(String checkName) {
        return config.getInt("anticheat.checks." + checkName + ".max-violations", 3);
    }
}
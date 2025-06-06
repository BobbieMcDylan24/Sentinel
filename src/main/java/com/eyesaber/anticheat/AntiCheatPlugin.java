package com.eyesaber.anticheat;

import com.eyesaber.anticheat.commands.AntiCheatCommand;
import com.eyesaber.anticheat.listeners.PlayerListener;
import com.eyesaber.anticheat.listeners.MovementListener;
import com.eyesaber.anticheat.managers.AlertManager;
import com.eyesaber.anticheat.managers.CheckManager;
import com.eyesaber.anticheat.managers.ConfigManager;
import com.eyesaber.anticheat.managers.ViolationManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiCheatPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private CheckManager checkManager;
    private ViolationManager violationManager;
    private AlertManager alertManager;
    
    @Override
    public void onEnable() {
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.violationManager = new ViolationManager(this);
        this.alertManager = new AlertManager(this);
        this.checkManager = new CheckManager(this);
        
        // Register events
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MovementListener(this), this);
        
        // Register commands
        getCommand("anticheat").setExecutor(new AntiCheatCommand(this));
        
        // Startup message
        getLogger().info("========================================");
        getLogger().info("  SentinelAC v" + getDescription().getVersion());
        getLogger().info("  Successfully loaded and protecting server!");
        getLogger().info("  Author: " + getDescription().getAuthors().get(0));
        getLogger().info("========================================");
        
        // Load configuration
        configManager.loadConfig();
        
        getLogger().info("SentinelAC has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("SentinelAC has been disabled. Server is no longer protected!");
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CheckManager getCheckManager() {
        return checkManager;
    }
    
    public ViolationManager getViolationManager() {
        return violationManager;
    }
    
    public AlertManager getAlertManager() {
        return alertManager;
    }
}
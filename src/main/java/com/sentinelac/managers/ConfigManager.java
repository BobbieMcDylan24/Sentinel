package com.sentinelac.managers;

import com.sentinelac.SentinelAC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class ConfigManager {

    private final SentinelAC plugin;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Config caching for performance
    private final ConcurrentHashMap<String, Object> configCache = new ConcurrentHashMap<>();
    
    // Config files
    private FileConfiguration config;
    private FileConfiguration checksConfig;
    private FileConfiguration alertsConfig;
    
    private File configFile;
    private File checksFile;
    private File alertsFile;

    public ConfigManager(SentinelAC plugin) {
        this.plugin = plugin;
        initialize();
    }
    
    private void initialize() {
        plugin.getLogger().info("Loading configuration files...");
        
        try {
            // Create plugin data folder
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Initialize main config
            initializeMainConfig();
            
            // Initialize checks config
            initializeChecksConfig();
            
            // Initialize alerts config
            initializeAlertsConfig();
            
            // Cache frequently accessed values
            cacheValues();
            
            plugin.getLogger().info("Configuration loaded successfully");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration", e);
            throw new RuntimeException("Configuration initialization failed", e);
        }
    }
    
    private void initializeMainConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        
        config = plugin.getConfig();
        
        // Set default values if not present
        setDefaults();
        save();
    }
    
    private void initializeChecksConfig() {
        checksFile = new File(plugin.getDataFolder(), "checks.yml");
        
        if (!checksFile.exists()) {
            plugin.saveResource("checks.yml", false);
        }
        
        checksConfig = YamlConfiguration.loadConfiguration(checksFile);
    }
    
    private void initializeAlertsConfig() {
        alertsFile = new File(plugin.getDataFolder(), "alerts.yml");
        
        if (!alertsFile.exists()) {
            plugin.saveResource("alerts.yml", false);
        }
        
        alertsConfig = YamlConfiguration.loadConfiguration(alertsFile);
    }
    
    private void setDefaults() {
        // General settings
        config.addDefault("general.debug", false);
        config.addDefault("general.async-checks", true);
        config.addDefault("general.max-ping", 1000);
        config.addDefault("general.check-exempt-permission", "sentinelac.exempt");
        
        // Threading settings
        config.addDefault("threading.packet-thread-size", 2);
        config.addDefault("threading.check-thread-size", 4);
        
        // Violation settings
        config.addDefault("violations.max-violations", 10);
        config.addDefault("violations.violation-reset-time", 300);
        config.addDefault("violations.auto-ban-threshold", 15);
        
        // Alert settings
        config.addDefault("alerts.enabled", true);
        config.addDefault("alerts.webhook.enabled", false);
        config.addDefault("alerts.webhook.url", "");
        config.addDefault("alerts.console", true);
        config.addDefault("alerts.staff-permission", "sentinelac.alerts");
        
        config.options().copyDefaults(true);
    }
    
    private void cacheValues() {
        lock.writeLock().lock();
        try {
            configCache.clear();
            
            // Cache frequently accessed values for performance
            configCache.put("debug", config.getBoolean("general.debug"));
            configCache.put("async-checks", config.getBoolean("general.async-checks"));
            configCache.put("max-ping", config.getInt("general.max-ping"));
            configCache.put("alerts-enabled", config.getBoolean("alerts.enabled"));
            configCache.put("console-alerts", config.getBoolean("alerts.console"));
            configCache.put("max-violations", config.getInt("violations.max-violations"));
            configCache.put("auto-ban-threshold", config.getInt("violations.auto-ban-threshold"));
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void reload() {
        lock.writeLock().lock();
        try {
            plugin.reloadConfig();
            config = plugin.getConfig();
            
            checksConfig = YamlConfiguration.loadConfiguration(checksFile);
            alertsConfig = YamlConfiguration.loadConfiguration(alertsFile);
            
            cacheValues();
            
            plugin.getLogger().info("Configuration reloaded successfully");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload configuration", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void save() {
        lock.readLock().lock();
        try {
            plugin.saveConfig();
            
            if (checksConfig != null && checksFile != null) {
                checksConfig.save(checksFile);
            }
            
            if (alertsConfig != null && alertsFile != null) {
                alertsConfig.save(alertsFile);
            }
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Cached getters for performance
    public boolean isDebugEnabled() {
        return getCachedBoolean("debug");
    }
    
    public boolean isAsyncChecksEnabled() {
        return getCachedBoolean("async-checks");
    }
    
    public int getMaxPing() {
        return getCachedInt("max-ping");
    }
    
    public boolean areAlertsEnabled() {
        return getCachedBoolean("alerts-enabled");
    }
    
    public boolean areConsoleAlertsEnabled() {
        return getCachedBoolean("console-alerts");
    }
    
    public int getMaxViolations() {
        return getCachedInt("max-violations");
    }
    
    public int getAutoBanThreshold() {
        return getCachedInt("auto-ban-threshold");
    }
    
    // Generic getters
    public String getString(String path) {
        lock.readLock().lock();
        try {
            return config.getString(path);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public int getInt(String path) {
        lock.readLock().lock();
        try {
            return config.getInt(path);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean getBoolean(String path) {
        lock.readLock().lock();
        try {
            return config.getBoolean(path);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Check-specific getters
    public boolean isCheckEnabled(String checkName) {
        lock.readLock().lock();
        try {
            return checksConfig.getBoolean("checks." + checkName + ".enabled", true);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public int getCheckMaxViolations(String checkName) {
        lock.readLock().lock();
        try {
            return checksConfig.getInt("checks." + checkName + ".max-violations", 5);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Alert-specific getters
    public String getAlertFormat(String alertType) {
        lock.readLock().lock();
        try {
            return alertsConfig.getString("alerts." + alertType + ".format", "&cSentinelAC &7» &f{player} &cfailed &f{check} &7({violations} violations)");
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Cache helpers
    private boolean getCachedBoolean(String key) {
        lock.readLock().lock();
        try {
            return (Boolean) configCache.getOrDefault(key, false);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private int getCachedInt(String key) {
        lock.readLock().lock();
        try {
            return (Integer) configCache.getOrDefault(key, 0);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void shutdown() {
        save();
        configCache.clear();
    }
    
    // Getters for config objects (for managers that need direct access)
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getChecksConfig() {
        return checksConfig;
    }
    
    public FileConfiguration getAlertsConfig() {
        return alertsConfig;
    }
}

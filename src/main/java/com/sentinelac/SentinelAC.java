package com.sentinelac;

import com.sentinelac.commands.SentinelCommand;
import com.sentinelac.listeners.PacketListener;
import com.sentinelac.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SentinelAC extends JavaPlugin {

    private static SentinelAC instance;
    
    // Core managers
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private CheckManager checkManager;
    private ViolationManager violationManager;
    private AlertManager alertManager;
    
    // Threading
    private ExecutorService executorService;
    
    // Listeners
    private PacketListener packetListener;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;
        
        getLogger().info("Starting SentinelAC initialization...");
        
        try {
            // Initialize threading
            initializeThreading();
            
            // Initialize managers in order
            initializeManagers();
            
            // Initialize listeners
            initializeListeners();
            
            // Initialize commands
            initializeCommands();
            
            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info(String.format("SentinelAC enabled successfully in %dms", loadTime));
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable SentinelAC", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling SentinelAC...");
        
        try {
            // Shutdown listeners
            if (packetListener != null) {
                packetListener.shutdown();
            }
            
            // Shutdown managers
            shutdownManagers();
            
            // Shutdown threading
            shutdownThreading();
            
            getLogger().info("SentinelAC disabled successfully");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during SentinelAC shutdown", e);
        }
        
        instance = null;
    }
    
    private void initializeThreading() {
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        executorService = Executors.newFixedThreadPool(threadCount, r -> {
            Thread thread = new Thread(r, "SentinelAC-Worker");
            thread.setDaemon(true);
            return thread;
        });
        
        getLogger().info("Initialized thread pool with " + threadCount + " threads");
    }
    
    private void initializeManagers() {
        getLogger().info("Initializing managers...");
        
        // Order matters - some managers depend on others
        configManager = new ConfigManager(this);
        playerDataManager = new PlayerDataManager(this);
        checkManager = new CheckManager(this);
        violationManager = new ViolationManager(this);
        alertManager = new AlertManager(this);
        
        getLogger().info("All managers initialized successfully");
    }
    
    private void initializeListeners() {
        getLogger().info("Initializing listeners...");
        
        packetListener = new PacketListener(this);
        packetListener.initialize();
        
        getLogger().info("Listeners initialized successfully");
    }
    
    private void initializeCommands() {
        getLogger().info("Initializing commands...");
        
        SentinelCommand sentinelCommand = new SentinelCommand(this);
        getCommand("sentinel").setExecutor(sentinelCommand);
        getCommand("sentinel").setTabCompleter(sentinelCommand);
        
        getLogger().info("Commands initialized successfully");
    }
    
    private void shutdownManagers() {
        if (alertManager != null) {
            alertManager.shutdown();
        }
        if (violationManager != null) {
            violationManager.shutdown();
        }
        if (checkManager != null) {
            checkManager.shutdown();
        }
        if (playerDataManager != null) {
            playerDataManager.shutdown();
        }
        if (configManager != null) {
            configManager.shutdown();
        }
    }
    
    private void shutdownThreading() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    getLogger().warning("Thread pool did not terminate gracefully, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
        }
    }
    
    // Getters
    public static SentinelAC getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
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
    
    public ExecutorService getExecutorService() {
        return executorService;
    }
}

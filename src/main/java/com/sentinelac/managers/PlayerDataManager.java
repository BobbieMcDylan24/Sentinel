package com.sentinelac.managers;

import com.sentinelac.SentinelAC;
import com.sentinelac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PlayerDataManager implements Listener {

    private final SentinelAC plugin;
    private final ConcurrentHashMap<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupService;

    public PlayerDataManager(SentinelAC plugin) {
        this.plugin = plugin;
        this.cleanupService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "SentinelAC-Cleanup");
            thread.setDaemon(true);
            return thread;
        });
        
        initialize();
    }
    
    private void initialize() {
        plugin.getLogger().info("Initializing player data manager...");
        
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Create data for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            createPlayerData(player);
        }
        
        // Start cleanup task - runs every 5 minutes
        cleanupService.scheduleAtFixedRate(this::cleanupInactiveData, 5, 5, TimeUnit.MINUTES);
        
        plugin.getLogger().info("Player data manager initialized with " + playerDataMap.size() + " players");
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        createPlayerData(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayerData(event.getPlayer().getUniqueId(), false);
    }
    
    private void createPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!playerDataMap.containsKey(uuid)) {
            PlayerData data = new PlayerData(player);
            playerDataMap.put(uuid, data);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Created player data for " + player.getName());
            }
        }
    }
    
    private void removePlayerData(UUID uuid, boolean force) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            // Only remove if player is offline or forced
            if (force || !data.isOnline()) {
                playerDataMap.remove(uuid);
                data.cleanup();
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Removed player data for " + uuid);
                }
            }
        }
    }
    
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        
        // If data doesn't exist and player is online, create it
        if (data == null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                createPlayerData(player);
                data = playerDataMap.get(uuid);
            }
        }
        
        return data;
    }
    
    public boolean hasPlayerData(UUID uuid) {
        return playerDataMap.containsKey(uuid);
    }
    
    public int getPlayerCount() {
        return playerDataMap.size();
    }
    
    private void cleanupInactiveData() {
        try {
            int removed = 0;
            long currentTime = System.currentTimeMillis();
            
            playerDataMap.entrySet().removeIf(entry -> {
                PlayerData data = entry.getValue();
                
                // Remove data if player has been offline for more than 10 minutes
                if (!data.isOnline() && (currentTime - data.getLastSeen()) > TimeUnit.MINUTES.toMillis(10)) {
                    data.cleanup();
                    return true;
                }
                
                return false;
            });
            
            if (removed > 0 && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Cleaned up " + removed + " inactive player data entries");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during player data cleanup", e);
        }
    }
    
    public void updatePlayerPing(UUID uuid, int ping) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            data.setPing(ping);
        }
    }
    
    public void updatePlayerTPS(double tps) {
        // Update TPS for all player data
        for (PlayerData data : playerDataMap.values()) {
            data.setServerTPS(tps);
        }
    }
    
    public void forceCleanup() {
        cleanupInactiveData();
    }
    
    public void shutdown() {
        plugin.getLogger().info("Shutting down player data manager...");
        
        // Cleanup all player data
        for (PlayerData data : playerDataMap.values()) {
            data.cleanup();
        }
        playerDataMap.clear();
        
        // Shutdown cleanup service
        cleanupService.shutdown();
        try {
            if (!cleanupService.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupService.shutdownNow();
        }
        
        plugin.getLogger().info("Player data manager shutdown complete");
    }
    
    // Debug and monitoring methods
    public void printPlayerDataStats() {
        plugin.getLogger().info("=== Player Data Statistics ===");
        plugin.getLogger().info("Total players tracked: " + playerDataMap.size());
        
        int onlineCount = 0;
        int highPingCount = 0;
        
        for (PlayerData data : playerDataMap.values()) {
            if (data.isOnline()) {
                onlineCount++;
            }
            if (data.getPing() > plugin.getConfigManager().getMaxPing()) {
                highPingCount++;
            }
        }
        
        plugin.getLogger().info("Online players: " + onlineCount);
        plugin.getLogger().info("High ping players: " + highPingCount);
        plugin.getLogger().info("==============================");
    }
}

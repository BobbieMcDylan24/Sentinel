package com.eyesaber.anticheat.managers;

import com.eyesaber.anticheat.AntiCheatPlugin;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViolationManager {
    
    private final AntiCheatPlugin plugin;
    private final Map<UUID, Map<String, Integer>> violations;
    
    public ViolationManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.violations = new ConcurrentHashMap<>();
    }
    
    public void addViolation(Player player, String checkName) {
        UUID uuid = player.getUniqueId();
        violations.computeIfAbsent(uuid, k -> new HashMap<>());
        
        Map<String, Integer> playerViolations = violations.get(uuid);
        int currentViolations = playerViolations.getOrDefault(checkName, 0) + 1;
        playerViolations.put(checkName, currentViolations);
        
        plugin.getLogger().info(player.getName() + " failed " + checkName + " (x" + currentViolations + ")");
    }
    
    public int getViolations(Player player, String checkName) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> playerViolations = violations.get(uuid);
        if (playerViolations == null) {
            return 0;
        }
        return playerViolations.getOrDefault(checkName, 0);
    }
    
    public void clearViolations(Player player) {
        violations.remove(player.getUniqueId());
    }
    
    public void clearViolations(Player player, String checkName) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> playerViolations = violations.get(uuid);
        if (playerViolations != null) {
            playerViolations.remove(checkName);
        }
    }
    
    public Map<String, Integer> getPlayerViolations(Player player) {
        UUID uuid = player.getUniqueId();
        return violations.getOrDefault(uuid, new HashMap<>());
    }
    
    public void resetAllViolations() {
        violations.clear();
    }
}
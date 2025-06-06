package com.eyesaber.anticheat.checks.movement;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.Check;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NoFallCheck extends Check {
    
    private final Map<UUID, Double> fallDistances = new HashMap<>();
    private final Map<UUID, Integer> noFallViolations = new HashMap<>();
    private final Map<UUID, Long> lastGroundTime = new HashMap<>();
    
    public NoFallCheck(AntiCheatPlugin plugin) {
        super(plugin, "nofall");
    }
    
    public void check(Player player, PlayerMoveEvent event) {
        if (!isEnabled()) {
            return;
        }
        
        // Skip for creative/spectator mode
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        
        // Skip if player has bypass permission
        if (player.hasPermission("anticheat.bypass") || player.hasPermission("anticheat.bypass.nofall")) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        Location to = event.getTo();
        Location from = event.getFrom();
        
        if (to == null) return;
        
        boolean isOnGround = isPlayerOnGround(player);
        boolean isInLiquid = isPlayerInLiquid(player);
        double deltaY = to.getY() - from.getY();
        
        // Track fall distance
        if (!isOnGround && !isInLiquid && deltaY < 0) {
            double currentFall = fallDistances.getOrDefault(uuid, 0.0);
            fallDistances.put(uuid, currentFall + Math.abs(deltaY));
        } else if (isOnGround || isInLiquid) {
            // Player landed or is in liquid
            double fallDistance = fallDistances.getOrDefault(uuid, 0.0);
            double serverFallDistance = player.getFallDistance();
            
            // Check for NoFall hack
            double minFallDistance = plugin.getConfigManager().getConfig()
                .getDouble("anticheat.checks.nofall.min-fall-distance", 3.0);
            
            if (fallDistance > minFallDistance && serverFallDistance < 0.5) {
                int violations = noFallViolations.getOrDefault(uuid, 0) + 1;
                noFallViolations.put(uuid, violations);
                
                if (violations >= 2) {
                    flag(player, String.format("NoFall detected (Fall: %.2f, Server: %.2f, VL: %d)", 
                        fallDistance, serverFallDistance, violations));
                    
                    // Apply fall damage manually
                    if (fallDistance > 3.0) {
                        double damage = Math.min(fallDistance - 3.0, 20.0);
                        player.damage(damage);
                    }
                }
            } else {
                // Reduce violations if legitimate fall
                int violations = noFallViolations.getOrDefault(uuid, 0);
                if (violations > 0) {
                    noFallViolations.put(uuid, Math.max(0, violations - 1));
                }
            }
            
            // Reset fall distance
            fallDistances.put(uuid, 0.0);
            lastGroundTime.put(uuid, System.currentTimeMillis());
        }
    }
    
    private boolean isPlayerOnGround(Player player) {
        Location loc = player.getLocation();
        
        // Check blocks below player
        for (double y = 0.1; y <= 1.0; y += 0.1) {
            Location checkLoc = loc.clone().subtract(0, y, 0);
            Material material = checkLoc.getBlock().getType();
            
            if (material != Material.AIR && material.isSolid()) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isPlayerInLiquid(Player player) {
        Location loc = player.getLocation();
        Material material = loc.getBlock().getType();
        Material headMaterial = loc.clone().add(0, 1, 0).getBlock().getType();
        
        return material.name().contains("WATER") || material.name().contains("LAVA") ||
               headMaterial.name().contains("WATER") || headMaterial.name().contains("LAVA");
    }
    
    public void clearViolations(Player player) {
        UUID uuid = player.getUniqueId();
        noFallViolations.remove(uuid);
        fallDistances.remove(uuid);
        lastGroundTime.remove(uuid);
    }
}
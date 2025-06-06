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

public class FlyCheck extends Check {
    
    private final Map<UUID, Integer> airTime = new HashMap<>();
    private final Map<UUID, Integer> flyViolations = new HashMap<>();
    private final Map<UUID, Long> lastGroundTime = new HashMap<>();
    
    public FlyCheck(AntiCheatPlugin plugin) {
        super(plugin, "fly");
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
        if (player.hasPermission("anticheat.bypass") || player.hasPermission("anticheat.bypass.fly")) {
            return;
        }
        
        // Skip if player is using elytra and config allows it
        if (player.isGliding() && plugin.getConfigManager().getConfig().getBoolean("anticheat.checks.fly.check-elytra", true)) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        Location to = event.getTo();
        Location from = event.getFrom();
        
        if (to == null) return;
        
        boolean isOnGround = isPlayerOnGround(player);
        boolean isInLiquid = isPlayerInLiquid(player);
        double deltaY = to.getY() - from.getY();
        
        if (isOnGround || isInLiquid) {
            // Player is on ground or in liquid
            airTime.put(uuid, 0);
            lastGroundTime.put(uuid, System.currentTimeMillis());
            
            // Reduce violations when on ground
            int violations = flyViolations.getOrDefault(uuid, 0);
            if (violations > 0) {
                flyViolations.put(uuid, Math.max(0, violations - 1));
            }
        } else {
            // Player is in air
            int currentAirTime = airTime.getOrDefault(uuid, 0) + 1;
            airTime.put(uuid, currentAirTime);
            
            // Check for suspicious flight patterns
            if (currentAirTime > 20 && deltaY >= 0) { // In air for 1+ second and not falling
                int violations = flyViolations.getOrDefault(uuid, 0) + 1;
                flyViolations.put(uuid, violations);
                
                // Vulcan kicks at 10 violations for flight
                int maxViolations = plugin.getCheckManager().getMaxViolations("fly");
                
                if (violations >= maxViolations) {
                    flag(player, String.format("Flight (Air: %d ticks, DeltaY: %.2f)", currentAirTime, deltaY));
                    
                    // Teleport to ground if enabled
                    if (plugin.getConfigManager().getConfig().getBoolean("anticheat.checks.fly.teleport-to-ground", true)) {
                        teleportToGround(player);
                    }
                    
                    flyViolations.put(uuid, 0); // Reset after punishment
                } else if (violations % 3 == 0) {
                    // Send alert every 3 violations
                    flag(player, String.format("Flight (Air: %d ticks, DeltaY: %.2f)", currentAirTime, deltaY));
                }
            }
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
    
    private void teleportToGround(Player player) {
        Location loc = player.getLocation();
        
        // Find safe ground location
        for (int y = (int) loc.getY(); y > 0; y--) {
            Location groundLoc = new Location(loc.getWorld(), loc.getX(), y, loc.getZ());
            Material groundMaterial = groundLoc.getBlock().getType();
            Material aboveMaterial = groundLoc.clone().add(0, 1, 0).getBlock().getType();
            
            if (groundMaterial.isSolid() && aboveMaterial == Material.AIR) {
                player.teleport(groundLoc.add(0, 1, 0));
                break;
            }
        }
    }
    
    public void clearViolations(Player player) {
        UUID uuid = player.getUniqueId();
        flyViolations.remove(uuid);
        airTime.remove(uuid);
        lastGroundTime.remove(uuid);
    }
}
package com.sentinelac.checks.movement;

import com.sentinelac.SentinelAC;
import com.sentinelac.checks.Check;
import com.sentinelac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class FlightCheck extends Check {

    private static final double MAX_AIRBORNE_TIME = 5000; // 5 seconds
    private static final double GRAVITY_ACCELERATION = 0.08;
    private static final double DRAG_MULTIPLIER = 0.98;
    private static final double MIN_Y_MOTION_THRESHOLD = -0.5;
    
    public FlightCheck(SentinelAC plugin) {
        super(plugin, "Flight", "Detects illegal flight and hovering");
        this.movementCheck = true;
    }

    @Override
    public void checkMovement(Player player, PlayerData data) {
        if (isExempt(player, data) || shouldIgnorePlayer(player, data)) {
            return;
        }

        Location current = data.getCurrentLocation();
        Location last = data.getLastLocation();
        
        if (current == null || last == null) {
            return;
        }

        // Don't check players in creative or spectator mode
        if (player.isFlying() || player.getAllowFlight()) {
            resetAirborneTime(data);
            return;
        }

        // Check if player is on ground or in liquid
        if (isPlayerGrounded(player, current) || isInLiquid(player, current)) {
            resetAirborneTime(data);
            return;
        }

        // Check if player is near climbable blocks
        if (isNearClimbable(current)) {
            resetAirborneTime(data);
            return;
        }

        // Update airborne time
        updateAirborneTime(data);
        long airborneTime = getAirborneTime(data);
        
        // Check for flight patterns
        checkHoveringPattern(player, data, current, last);
        checkVerticalAcceleration(player, data, current, last, airborneTime);
        checkAirborneTime(player, data, airborneTime);
    }

    private void checkHoveringPattern(Player player, PlayerData data, Location current, Location last) {
        double verticalMotion = current.getY() - last.getY();
        
        // Check for repeated small vertical movements (hovering)
        Integer hoverCount = data.getCheckData("flight.hoverCount", Integer.class);
        if (hoverCount == null) hoverCount = 0;
        
        Double lastVerticalMotion = data.getCheckData("flight.lastVerticalMotion", Double.class);
        if (lastVerticalMotion == null) lastVerticalMotion = 0.0;
        
        // Detect hovering pattern: small vertical movements around the same Y level
        if (Math.abs(verticalMotion) < 0.1 && Math.abs(lastVerticalMotion) < 0.1) {
            hoverCount++;
            data.setCheckData("flight.hoverCount", hoverCount);
            
            if (hoverCount > 10) {
                flag(player, data, String.format("Hovering pattern detected (count: %d, vMotion: %.3f)", 
                    hoverCount, verticalMotion));
                hoverCount = 0;
            }
        } else {
            hoverCount = Math.max(0, hoverCount - 1);
            data.setCheckData("flight.hoverCount", hoverCount);
        }
        
        data.setCheckData("flight.lastVerticalMotion", verticalMotion);
    }

    private void checkVerticalAcceleration(Player player, PlayerData data, Location current, Location last, long airborneTime) {
        double verticalMotion = current.getY() - last.getY();
        Double expectedMotion = data.getCheckData("flight.expectedMotion", Double.class);
        
        if (expectedMotion == null) {
            // Initialize expected motion
            expectedMotion = verticalMotion;
        } else {
            // Apply gravity and drag to expected motion
            expectedMotion = (expectedMotion - GRAVITY_ACCELERATION) * DRAG_MULTIPLIER;
            
            // Account for jump boost
            if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
            int amplifier = player.getPotionEffect(PotionEffectType.JUMP_BOOST).getAmplifier();
                expectedMotion += 0.1 * (amplifier + 1);
            }
        }
        
        data.setCheckData("flight.expectedMotion", expectedMotion);
        
        // Check if actual motion significantly differs from expected motion
        if (airborneTime > 1000) { // Only check after being airborne for 1 second
            double motionDifference = Math.abs(verticalMotion - expectedMotion);
            
            if (motionDifference > 0.5 && verticalMotion > MIN_Y_MOTION_THRESHOLD) {
                flag(player, data, String.format("Impossible vertical motion (expected: %.3f, actual: %.3f, diff: %.3f)", 
                    expectedMotion, verticalMotion, motionDifference));
            }
        }
    }

    private void checkAirborneTime(Player player, PlayerData data, long airborneTime) {
        if (airborneTime > MAX_AIRBORNE_TIME) {
            flag(player, data, String.format("Excessive airborne time (%.2fs)", airborneTime / 1000.0));
            
            // Reset to prevent spam
            resetAirborneTime(data);
        }
    }

    private boolean shouldIgnorePlayer(Player player, PlayerData data) {
        // Ignore lagging players
        if (isPlayerLagging(data) || isServerLagging(data)) {
            return true;
        }
        
        // Ignore players with elytra
        if (player.isGliding()) {
            return true;
        }
        
        // Ignore players in vehicles
        if (player.isInsideVehicle()) {
            return true;
        }
        
        // Ignore players with levitation effect
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            return true;
        }
        
        // Ignore players with slow falling
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return true;
        }
        
        return false;
    }

    private boolean isPlayerGrounded(Player player, Location location) {
        // Check block directly below player
        Location below = location.clone().subtract(0, 1, 0);
        Material blockBelow = below.getBlock().getType();
        
        // Check if standing on solid block
        if (blockBelow.isSolid() && !blockBelow.isTransparent()) {
            return true;
        }
        
        // Check for partial blocks (slabs, stairs, etc.)
        if (isPartialBlock(blockBelow)) {
            return true;
        }
        
        // Check for blocks player can stand on (like fences)
        if (blockBelow == Material.COBBLESTONE_WALL || 
            blockBelow.name().contains("FENCE") ||
            blockBelow.name().contains("GATE")) {
            return true;
        }
        
        return false;
    }

    private boolean isInLiquid(Player player, Location location) {
        Material blockType = location.getBlock().getType();
        return blockType == Material.WATER || blockType == Material.LAVA;
    }

    private boolean isNearClimbable(Location location) {
        // Check surrounding blocks for climbable materials
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location check = location.clone().add(x, y, z);
                    Material material = check.getBlock().getType();
                    
                    if (material == Material.LADDER || 
                        material == Material.VINE ||
                        material.name().contains("SCAFFOLDING")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPartialBlock(Material material) {
        return material.name().contains("SLAB") ||
               material.name().contains("STAIRS") ||
               material.name().contains("CARPET") ||
               material == Material.SOUL_SAND ||
               material == Material.FARMLAND ||
               material == Material.DIRT_PATH;
    }

    private void updateAirborneTime(PlayerData data) {
        Long airborneStart = data.getCheckData("flight.airborneStart", Long.class);
        if (airborneStart == null) {
            data.setCheckData("flight.airborneStart", System.currentTimeMillis());
        }
    }

    private long getAirborneTime(PlayerData data) {
        Long airborneStart = data.getCheckData("flight.airborneStart", Long.class);
        if (airborneStart == null) {
            return 0;
        }
        return System.currentTimeMillis() - airborneStart;
    }

    private void resetAirborneTime(PlayerData data) {
        data.setCheckData("flight.airborneStart", null);
        data.setCheckData("flight.expectedMotion", null);
        data.setCheckData("flight.hoverCount", 0);
    }
}

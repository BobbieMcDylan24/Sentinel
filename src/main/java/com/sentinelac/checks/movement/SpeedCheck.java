package com.sentinelac.checks.movement;

import com.sentinelac.SentinelAC;
import com.sentinelac.checks.Check;
import com.sentinelac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class SpeedCheck extends Check {

    private static final double MAX_HORIZONTAL_SPEED = 0.6; // Blocks per tick
    private static final double MAX_SPRINT_SPEED = 0.8;
    private static final double MAX_VERTICAL_SPEED = 1.2;
    private static final double SPEED_BUFFER_THRESHOLD = 1.5;
    private static final int BUFFER_DECAY_TICKS = 20;
    
    public SpeedCheck(SentinelAC plugin) {
        super(plugin, "Speed", "Detects horizontal and vertical speed hacks");
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

        // Don't check creative/spectator players
        if (player.isFlying() || player.getAllowFlight()) {
            return;
        }

        checkHorizontalSpeed(player, data, current, last);
        checkVerticalSpeed(player, data, current, last);
        
        // Decay speed buffer over time
        decaySpeedBuffer(data);
    }

    private void checkHorizontalSpeed(Player player, PlayerData data, Location current, Location last) {
        double horizontalDistance = data.getHorizontalDistance();
        
        // Calculate maximum allowed speed
        double maxSpeed = calculateMaxHorizontalSpeed(player, current);
        
        // Get speed buffer
        Double speedBuffer = data.getCheckData("speed.horizontalBuffer", Double.class);
        if (speedBuffer == null) speedBuffer = 0.0;
        
        if (horizontalDistance > maxSpeed) {
            double excess = horizontalDistance - maxSpeed;
            speedBuffer += excess;
            
            // Check if buffer exceeds threshold
            if (speedBuffer > SPEED_BUFFER_THRESHOLD) {
                flag(player, data, String.format("Horizontal speed hack (speed: %.3f, max: %.3f, buffer: %.3f)", 
                    horizontalDistance, maxSpeed, speedBuffer));
                speedBuffer *= 0.8; // Reduce buffer on flag
            }
        } else {
            // Player is within limits, reduce buffer
            speedBuffer = Math.max(0, speedBuffer - 0.05);
        }
        
        data.setCheckData("speed.horizontalBuffer", speedBuffer);
    }

    private void checkVerticalSpeed(Player player, PlayerData data, Location current, Location last) {
        double verticalDistance = Math.abs(data.getVerticalDistance());
        
        // Only check upward movement (positive Y)
        if (data.getVerticalDistance() <= 0) {
            return;
        }
        
        // Calculate maximum allowed vertical speed
        double maxVerticalSpeed = calculateMaxVerticalSpeed(player, current);
        
        // Get vertical buffer
        Double verticalBuffer = data.getCheckData("speed.verticalBuffer", Double.class);
        if (verticalBuffer == null) verticalBuffer = 0.0;
        
        if (verticalDistance > maxVerticalSpeed) {
            double excess = verticalDistance - maxVerticalSpeed;
            verticalBuffer += excess;
            
            // Check if buffer exceeds threshold
            if (verticalBuffer > SPEED_BUFFER_THRESHOLD) {
                flag(player, data, String.format("Vertical speed hack (speed: %.3f, max: %.3f, buffer: %.3f)", 
                    verticalDistance, maxVerticalSpeed, verticalBuffer));
                verticalBuffer *= 0.8; // Reduce buffer on flag
            }
        } else {
            // Player is within limits, reduce buffer
            verticalBuffer = Math.max(0, verticalBuffer - 0.05);
        }
        
        data.setCheckData("speed.verticalBuffer", verticalBuffer);
    }

    private double calculateMaxHorizontalSpeed(Player player, Location location) {
        double baseSpeed = MAX_HORIZONTAL_SPEED;
        
        // Check if player is sprinting
        if (player.isSprinting()) {
            baseSpeed = MAX_SPRINT_SPEED;
        }
        
        // Apply speed effect
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            baseSpeed *= 1.0 + ((amplifier + 1) * 0.2);
        }
        
        // Apply slowness effect
        if (player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SLOWNESS).getAmplifier();
            baseSpeed *= 1.0 - ((amplifier + 1) * 0.15);
        }
        
        // Check surface type
        Material blockBelow = location.clone().subtract(0, 1, 0).getBlock().getType();
        
        // Ice increases speed
        if (blockBelow == Material.ICE || blockBelow == Material.PACKED_ICE || blockBelow == Material.BLUE_ICE) {
            baseSpeed *= 1.3;
        }
        
        // Soul sand decreases speed
        if (blockBelow == Material.SOUL_SAND || blockBelow == Material.SOUL_SOIL) {
            baseSpeed *= 0.4;
        }
        
        // Honey blocks decrease speed
        if (blockBelow == Material.HONEY_BLOCK) {
            baseSpeed *= 0.4;
        }
        
        // Web decreases speed significantly
        if (location.getBlock().getType() == Material.COBWEB) {
            baseSpeed *= 0.05;
        }
        
        // Water decreases speed
        if (isInWater(location)) {
            baseSpeed *= 0.3;
            
            // Depth strider enchantment
            if (player.getInventory().getBoots() != null) {
                int depthStrider = player.getInventory().getBoots().getEnchantmentLevel(
                    org.bukkit.enchantments.Enchantment.DEPTH_STRIDER);
                if (depthStrider > 0) {
                    baseSpeed *= 1.0 + (depthStrider * 0.3);
                }
            }
        }
        
        // Lava decreases speed even more
        if (isInLava(location)) {
            baseSpeed *= 0.1;
        }
        
        // Account for hunger/food level
        if (player.getFoodLevel() <= 6) {
            baseSpeed *= 0.7; // Reduced speed when hungry
        }
        
        return baseSpeed;
    }

    private double calculateMaxVerticalSpeed(Player player, Location location) {
        double baseSpeed = MAX_VERTICAL_SPEED;
        
        // Apply jump boost effect
        if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
            int amplifier = player.getPotionEffect(PotionEffectType.JUMP_BOOST).getAmplifier();
            baseSpeed += (amplifier + 1) * 0.3;
        }
        
        // Check for jump blocks
        Material blockBelow = location.clone().subtract(0, 1, 0).getBlock().getType();
        if (blockBelow == Material.SLIME_BLOCK) {
            baseSpeed *= 2.0; // Slime blocks increase jump height
        }
        
        // Water reduces vertical speed
        if (isInWater(location)) {
            baseSpeed *= 0.8;
        }
        
        // Lava reduces vertical speed more
        if (isInLava(location)) {
            baseSpeed *= 0.5;
        }
        
        return baseSpeed;
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
        
        // Ignore players with riptide trident
        if (player.isRiptiding()) {
            return true;
        }
        
        // Ignore teleportation events (check if player moved too far)
        if (data.getDistanceMoved() > 50) {
            return true;
        }
        
        return false;
    }

    private boolean isInWater(Location location) {
        Material blockType = location.getBlock().getType();
        return blockType == Material.WATER;
    }

    private boolean isInLava(Location location) {
        Material blockType = location.getBlock().getType();
        return blockType == Material.LAVA;
    }

    private void decaySpeedBuffer(PlayerData data) {
        Long lastBufferDecay = data.getCheckData("speed.lastBufferDecay", Long.class);
        if (lastBufferDecay == null) {
            lastBufferDecay = System.currentTimeMillis();
            data.setCheckData("speed.lastBufferDecay", lastBufferDecay);
            return;
        }
        
        long timeSinceDecay = System.currentTimeMillis() - lastBufferDecay;
        
        // Decay buffer every second
        if (timeSinceDecay >= 1000) {
            Double horizontalBuffer = data.getCheckData("speed.horizontalBuffer", Double.class);
            Double verticalBuffer = data.getCheckData("speed.verticalBuffer", Double.class);
            
            if (horizontalBuffer != null && horizontalBuffer > 0) {
                horizontalBuffer = Math.max(0, horizontalBuffer - 0.1);
                data.setCheckData("speed.horizontalBuffer", horizontalBuffer);
            }
            
            if (verticalBuffer != null && verticalBuffer > 0) {
                verticalBuffer = Math.max(0, verticalBuffer - 0.1);
                data.setCheckData("speed.verticalBuffer", verticalBuffer);
            }
            
            data.setCheckData("speed.lastBufferDecay", System.currentTimeMillis());
        }
    }
}

package com.sentinelac.checks.movement;

import com.sentinelac.SentinelAC;
import com.sentinelac.checks.Check;
import com.sentinelac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

public class NoFallCheck extends Check {

    private static final double MIN_FALL_DISTANCE = 3.0;
    private static final double DAMAGE_PER_BLOCK = 0.5;
    private static final double FEATHER_FALLING_REDUCTION = 0.12;
    
    public NoFallCheck(SentinelAC plugin) {
        super(plugin, "NoFall", "Detects NoFall cheats and fall damage bypass");
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

        // Update fall distance tracking
        updateFallDistance(player, data, current, last);
        
        // Check for NoFall patterns
        checkNoFallDamage(player, data, current);
        checkOnGroundSpoofing(player, data, current);
        checkFallDistanceReset(player, data);
    }

    private void updateFallDistance(Player player, PlayerData data, Location current, Location last) {
        double verticalMotion = current.getY() - last.getY();
        
        // Get current fall distance
        Double trackedFallDistance = data.getCheckData("nofall.fallDistance", Double.class);
        if (trackedFallDistance == null) trackedFallDistance = 0.0;
        
        if (verticalMotion < 0) {
            // Player is falling
            trackedFallDistance += Math.abs(verticalMotion);
            data.setCheckData("nofall.falling", true);
        } else if (isPlayerGrounded(player, current)) {
            // Player has landed
            if (data.getCheckData("nofall.falling", Boolean.class) == Boolean.TRUE) {
                checkFallDamage(player, data, trackedFallDistance);
            }
            
            // Reset fall tracking
            trackedFallDistance = 0.0;
            data.setCheckData("nofall.falling", false);
        }
        
        data.setCheckData("nofall.fallDistance", trackedFallDistance);
        data.setCheckData("nofall.lastGrounded", isPlayerGrounded(player, current));
    }

    private void checkFallDamage(Player player, PlayerData data, double fallDistance) {
        if (fallDistance < MIN_FALL_DISTANCE) {
            return;
        }
        
        // Calculate expected damage
        double expectedDamage = calculateExpectedFallDamage(player, fallDistance);
        
        if (expectedDamage > 0) {
            // Player should have taken damage, check if they did
            Long lastDamageTime = data.getCheckData("nofall.lastDamageTime", Long.class);
            long currentTime = System.currentTimeMillis();
            
            // Check if player took damage within the last 500ms
            if (lastDamageTime == null || currentTime - lastDamageTime > 500) {
                flag(player, data, String.format("No fall damage taken (fall: %.2f blocks, expected damage: %.1f)", 
                    fallDistance, expectedDamage));
            }
        }
    }

    private void checkNoFallDamage(Player player, PlayerData data, Location current) {
        // Check if player claims to be on ground when they shouldn't be
        Boolean lastGrounded = data.getCheckData("nofall.lastGrounded", Boolean.class);
        if (lastGrounded == null) lastGrounded = false;
        
        boolean actuallyGrounded = isPlayerGrounded(player, current);
        
        // Get server-side onGround status (from packet data if available)
        Boolean clientOnGround = data.getCheckData("nofall.clientOnGround", Boolean.class);
        
        if (clientOnGround != null && clientOnGround && !actuallyGrounded) {
            Integer spoofCount = data.getCheckData("nofall.spoofCount", Integer.class);
            if (spoofCount == null) spoofCount = 0;
            
            spoofCount++;
            data.setCheckData("nofall.spoofCount", spoofCount);
            
            if (spoofCount > 5) {
                flag(player, data, String.format("OnGround spoofing detected (count: %d)", spoofCount));
                spoofCount = 0;
            }
        } else {
            data.setCheckData("nofall.spoofCount", 0);
        }
    }

    private void checkOnGroundSpoofing(Player player, PlayerData data, Location current) {
        // Track inconsistent ground state claims
        boolean actuallyGrounded = isPlayerGrounded(player, current);
        Boolean clientOnGround = data.getCheckData("nofall.clientOnGround", Boolean.class);
        
        if (clientOnGround != null) {
            if (clientOnGround && !actuallyGrounded) {
                // Client claims on ground but server says not
                Integer groundSpoof = data.getCheckData("nofall.groundSpoof", Integer.class);
                if (groundSpoof == null) groundSpoof = 0;
                
                groundSpoof++;
                data.setCheckData("nofall.groundSpoof", groundSpoof);
                
                if (groundSpoof > 10) {
                    flag(player, data, String.format("Ground state spoofing (spoof count: %d)", groundSpoof));
                    groundSpoof = 0;
                }
            } else {
                // Reset spoof counter when legitimate
                data.setCheckData("nofall.groundSpoof", 0);
            }
        }
    }

    private void checkFallDistanceReset(Player player, PlayerData data) {
        // Check for suspicious fall distance resets
        Double trackedFallDistance = data.getCheckData("nofall.fallDistance", Double.class);
        if (trackedFallDistance == null) trackedFallDistance = 0.0;
        
        if (trackedFallDistance > MIN_FALL_DISTANCE) {
            // Player is falling significant distance
            if (player.getFallDistance() < 1.0 && trackedFallDistance > 5.0) {
                flag(player, data, String.format("Suspicious fall distance reset (tracked: %.2f, client: %.2f)", 
                    trackedFallDistance, player.getFallDistance()));
            }
        }
    }

    private double calculateExpectedFallDamage(Player player, double fallDistance) {
        if (fallDistance < MIN_FALL_DISTANCE) {
            return 0.0;
        }
        
        double damage = (fallDistance - 3.0) * DAMAGE_PER_BLOCK;
        
        // Apply feather falling enchantment
        if (player.getInventory().getBoots() != null) {
            int featherFalling = player.getInventory().getBoots().getEnchantmentLevel(
                org.bukkit.enchantments.Enchantment.PROTECTION_FALL);
            if (featherFalling > 0) {
                damage *= Math.max(0, 1.0 - (featherFalling * FEATHER_FALLING_REDUCTION));
            }
        }
        
        // Apply jump boost effect (reduces fall damage)
        if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
            int amplifier = player.getPotionEffect(PotionEffectType.JUMP_BOOST).getAmplifier();
            damage = Math.max(0, damage - amplifier);
        }
        
        // Apply resistance effect
        if (player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
            int amplifier = player.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier();
            damage *= Math.max(0, 1.0 - ((amplifier + 1) * 0.2));
        }
        
        return Math.max(0, damage);
    }

    private boolean isPlayerGrounded(Player player, Location location) {
        // Check multiple points around the player's feet
        double[][] checkPoints = {
            {-0.3, 0, -0.3}, {0.3, 0, -0.3},
            {-0.3, 0, 0.3}, {0.3, 0, 0.3},
            {0, 0, 0} // Center point
        };
        
        for (double[] point : checkPoints) {
            Location checkLoc = location.clone().add(point[0], -0.1, point[2]);
            Material material = checkLoc.getBlock().getType();
            
            if (material.isSolid() && !material.isTransparent()) {
                return true;
            }
            
            // Check for partial blocks
            if (isPartialBlock(material)) {
                return true;
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
               material == Material.GRASS_PATH ||
               material.name().contains("FENCE") ||
               material.name().contains("WALL");
    }

    private boolean shouldIgnorePlayer(Player player, PlayerData data) {
        // Ignore lagging players
        if (isPlayerLagging(data) || isServerLagging(data)) {
            return true;
        }
        
        // Ignore creative/spectator players
        if (player.isFlying() || player.getAllowFlight()) {
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
        
        // Ignore players with slow falling effect
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return true;
        }
        
        // Ignore players with levitation effect
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            return true;
        }
        
        // Ignore players in water or lava
        Location loc = player.getLocation();
        Material blockType = loc.getBlock().getType();
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            return true;
        }
        
        // Ignore players on certain blocks that negate fall damage
        Location below = loc.clone().subtract(0, 1, 0);
        Material blockBelow = below.getBlock().getType();
        if (blockBelow == Material.SLIME_BLOCK || 
            blockBelow == Material.HAY_BLOCK ||
            blockBelow == Material.WATER ||
            blockBelow.name().contains("BED")) {
            return true;
        }
        
        return false;
    }

    // Method to be called when player takes damage (from damage listener)
    public void onPlayerDamage(Player player, PlayerData data, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            data.setCheckData("nofall.lastDamageTime", System.currentTimeMillis());
        }
    }

    // Method to be called from packet listener to track client onGround state
    public void updateClientOnGround(PlayerData data, boolean onGround) {
        data.setCheckData("nofall.clientOnGround", onGround);
    }
}

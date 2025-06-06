package com.eyesaber.anticheat.checks.block;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.Check;
import com.eyesaber.anticheat.utils.MaterialHelper;
import com.eyesaber.anticheat.utils.VersionUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FastBreakCheck extends Check {
    
    private final Map<UUID, Long> breakStartTime = new HashMap<>();
    private final Map<UUID, Block> lastBlock = new HashMap<>();
    private final Map<UUID, Integer> fastBreakViolations = new HashMap<>();
    
    public FastBreakCheck(AntiCheatPlugin plugin) {
        super(plugin, "fastbreak");
    }
    
    public void check(Player player, BlockBreakEvent event) {
        if (!isEnabled() || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        // Skip if player has bypass permission
        if (player.hasPermission("anticheat.bypass") || player.hasPermission("anticheat.bypass.fastbreak")) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        Block block = event.getBlock();
        long currentTime = System.currentTimeMillis();
        
        Block prevBlock = lastBlock.get(uuid);
        if (prevBlock != null && prevBlock.equals(block)) {
            Long startTime = breakStartTime.get(uuid);
            if (startTime != null) {
                long breakTime = currentTime - startTime;
                long expectedTime = calculateExpectedBreakTime(player, block);
                
                double tolerance = plugin.getConfigManager().getConfig()
                    .getDouble("anticheat.checks.fastbreak.tolerance", 0.8);
                
                if (breakTime < expectedTime * tolerance) {
                    int violations = fastBreakViolations.getOrDefault(uuid, 0) + 1;
                    fastBreakViolations.put(uuid, violations);
                    
                    // Vulcan kicks at 12 violations for fastbreak
                    int maxViolations = plugin.getCheckManager().getMaxViolations("fastbreak");
                    
                    if (violations >= maxViolations) {
                        flag(player, String.format("FastBreak (Time: %dms, Expected: %dms)", breakTime, expectedTime));
                        fastBreakViolations.put(uuid, 0); // Reset after punishment
                    } else if (violations % 4 == 0) {
                        // Send alert every 4 violations
                        flag(player, String.format("FastBreak (Time: %dms, Expected: %dms)", breakTime, expectedTime));
                    }
                } else {
                    // Reduce violations for legitimate breaks
                    int violations = fastBreakViolations.getOrDefault(uuid, 0);
                    if (violations > 0) {
                        fastBreakViolations.put(uuid, Math.max(0, violations - 1));
                    }
                }
            }
        }
        
        breakStartTime.put(uuid, currentTime);
        lastBlock.put(uuid, block);
    }
    
    private long calculateExpectedBreakTime(Player player, Block block) {
        Material blockType = block.getType();
        ItemStack tool = getPlayerTool(player);
        
        // Base break time
        long baseTime = getBaseBreakTime(blockType);
        
        // Tool efficiency
        if (tool != null && tool.getType() != Material.AIR) {
            // Check if tool is appropriate for block
            double toolMultiplier = getToolMultiplier(tool, blockType);
            baseTime = (long) (baseTime / toolMultiplier);
            
            // Enchantment efficiency
            Enchantment efficiency = VersionUtils.getEfficiencyEnchantment();
            if (efficiency != null && tool.containsEnchantment(efficiency)) {
                int efficiencyLevel = tool.getEnchantmentLevel(efficiency);
                double efficiencyMultiplier = 1 + (efficiencyLevel * 0.3);
                baseTime = (long) (baseTime / efficiencyMultiplier);
            }
        }
        
        // Haste effect
        PotionEffectType hasteEffect = VersionUtils.getHasteEffect();
        if (hasteEffect != null) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                if (effect.getType().equals(hasteEffect)) {
                    double hasteMultiplier = 1 + ((effect.getAmplifier() + 1) * 0.2);
                    baseTime = (long) (baseTime / hasteMultiplier);
                    break;
                }
            }
        }
        
        return Math.max(baseTime, 50); // Minimum 50ms
    }
    
    private ItemStack getPlayerTool(Player player) {
        try {
            // Modern method (1.9+)
            return player.getInventory().getItemInMainHand();
        } catch (Exception e) {
            // Legacy method (1.8)
            @SuppressWarnings("deprecation")
            ItemStack item = player.getInventory().getItemInHand();
            return item;
        }
    }
    
    private double getToolMultiplier(ItemStack tool, Material blockType) {
        String toolName = tool.getType().name();
        String blockName = blockType.name();
        
        // Pickaxe for stone/ore materials
        if (toolName.contains("PICKAXE")) {
            if (isStoneType(blockName) || isOreType(blockName)) {
                if (toolName.contains("DIAMOND")) return 8.0;
                if (toolName.contains("IRON")) return 6.0;
                if (toolName.contains("STONE")) return 4.0;
                if (toolName.contains("WOOD")) return 2.0;
                return 2.0; // Default pickaxe
            }
        }
        
        // Axe for wood materials
        if (toolName.contains("AXE")) {
            if (isWoodType(blockName)) {
                if (toolName.contains("DIAMOND")) return 8.0;
                if (toolName.contains("IRON")) return 6.0;
                if (toolName.contains("STONE")) return 4.0;
                if (toolName.contains("WOOD")) return 2.0;
                return 2.0; // Default axe
            }
        }
        
        // Shovel for dirt/sand materials
        if (toolName.contains("SHOVEL") || toolName.contains("SPADE")) {
            if (isDirtType(blockName) || isSandType(blockName)) {
                if (toolName.contains("DIAMOND")) return 8.0;
                if (toolName.contains("IRON")) return 6.0;
                if (toolName.contains("STONE")) return 4.0;
                if (toolName.contains("WOOD")) return 2.0;
                return 2.0; // Default shovel
            }
        }
        
        // Sword for certain blocks (web, leaves, etc.)
        if (toolName.contains("SWORD")) {
            if (blockName.contains("WEB") || blockName.contains("LEAVES") || 
                blockName.contains("VINE") || blockName.contains("BAMBOO")) {
                return 15.0; // Swords are very effective on these
            }
        }
        
        // Shears for specific blocks
        if (toolName.equals("SHEARS")) {
            if (blockName.contains("LEAVES") || blockName.contains("WOOL") || 
                blockName.contains("VINE") || blockName.equals("WEB")) {
                return 5.0;
            }
        }
        
        return 1.0; // No tool bonus
    }
    
    private long getBaseBreakTime(Material material) {
        String materialName = material.name();
        
        // Instant break blocks
        if (isInstantBreak(materialName)) {
            return 50; // Minimum time
        }
        
        // Very fast blocks (0.05 seconds)
        if (isFastBreak(materialName)) {
            return 100;
        }
        
        // Dirt-like blocks (0.5 seconds)
        if (isDirtType(materialName)) {
            return 500;
        }
        
        // Sand-like blocks (0.5 seconds)
        if (isSandType(materialName)) {
            return 500;
        }
        
        // Wood blocks (2.0 seconds)
        if (isWoodType(materialName)) {
            return 2000;
        }
        
        // Stone blocks (1.5 seconds)
        if (isStoneType(materialName)) {
            return 1500;
        }
        
        // Ore blocks (3.0 seconds)
        if (isOreType(materialName)) {
            return 3000;
        }
        
        // Hard blocks
        if (materialName.equals("OBSIDIAN")) {
            return 15000; // 15 seconds
        }
        
        if (materialName.equals("BEDROCK")) {
            return Integer.MAX_VALUE; // Unbreakable
        }
        
        if (materialName.contains("NETHERITE")) {
            return 4000;
        }
        
        if (materialName.contains("DIAMOND")) {
            return 3500;
        }
        
        if (materialName.contains("EMERALD")) {
            return 3500;
        }
        
        if (materialName.contains("GOLD")) {
            return 3000;
        }
        
        if (materialName.contains("IRON")) {
            return 2500;
        }
        
        if (materialName.contains("REDSTONE")) {
            return 3000;
        }
        
        if (materialName.contains("LAPIS")) {
            return 3000;
        }
        
        // Glass blocks (0.3 seconds)
        if (materialName.contains("GLASS")) {
            return 300;
        }
        
        // Wool blocks (0.8 seconds)
        if (materialName.contains("WOOL")) {
            return 800;
        }
        
        // Concrete blocks (1.8 seconds)
        if (materialName.contains("CONCRETE")) {
            return 1800;
        }
        
        // Terracotta blocks (1.25 seconds)
        if (materialName.contains("TERRACOTTA")) {
            return 1250;
        }
        
        // Default time for unknown blocks
        return 1000;
    }
    
    private boolean isInstantBreak(String materialName) {
        return materialName.contains("TORCH") || 
               materialName.contains("REDSTONE") ||
               materialName.contains("FLOWER") ||
               materialName.contains("GRASS") ||
               materialName.contains("MUSHROOM") ||
               materialName.equals("FIRE") ||
               materialName.equals("TNT") ||
               materialName.contains("SAPLING");
    }
    
    private boolean isFastBreak(String materialName) {
        return materialName.contains("LEAVES") ||
               materialName.equals("WEB") ||
               materialName.contains("VINE") ||
               materialName.contains("SNOW");
    }
    
    private boolean isDirtType(String materialName) {
        return materialName.contains("DIRT") ||
               materialName.contains("GRASS_BLOCK") ||
               materialName.contains("PODZOL") ||
               materialName.contains("MYCELIUM") ||
               materialName.contains("FARMLAND") ||
               materialName.contains("COARSE_DIRT");
    }
    
    private boolean isSandType(String materialName) {
        return materialName.contains("SAND") ||
               materialName.contains("GRAVEL") ||
               materialName.contains("SOUL_SAND") ||
               materialName.contains("SOUL_SOIL");
    }
    
    private boolean isWoodType(String materialName) {
        return materialName.contains("LOG") ||
               materialName.contains("WOOD") ||
               materialName.contains("PLANKS") ||
               materialName.contains("FENCE") ||
               materialName.contains("DOOR") ||
               materialName.contains("STAIRS") ||
               materialName.contains("SLAB") && materialName.contains("WOOD");
    }
    
    private boolean isStoneType(String materialName) {
        return materialName.contains("STONE") ||
               materialName.contains("COBBLESTONE") ||
               materialName.contains("GRANITE") ||
               materialName.contains("DIORITE") ||
               materialName.contains("ANDESITE") ||
               materialName.contains("DEEPSLATE") ||
               materialName.contains("BLACKSTONE") ||
               materialName.equals("NETHERRACK") ||
               materialName.equals("END_STONE");
    }
    
    private boolean isOreType(String materialName) {
        return materialName.contains("_ORE") ||
               materialName.contains("ANCIENT_DEBRIS") ||
               materialName.contains("NETHER_QUARTZ_ORE");
    }
    
    public void clearViolations(Player player) {
        UUID uuid = player.getUniqueId();
        fastBreakViolations.remove(uuid);
        breakStartTime.remove(uuid);
        lastBlock.remove(uuid);
    }
}
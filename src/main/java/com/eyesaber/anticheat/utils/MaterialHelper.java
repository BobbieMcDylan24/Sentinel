package com.eyesaber.anticheat.utils;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MaterialHelper {
    
    private static final Set<String> STONE_MATERIALS = new HashSet<>(Arrays.asList(
        "STONE", "COBBLESTONE", "MOSSY_COBBLESTONE", "SMOOTH_STONE",
        "STONE_BRICKS", "MOSSY_STONE_BRICKS", "CRACKED_STONE_BRICKS",
        "CHISELED_STONE_BRICKS", "GRANITE", "DIORITE", "ANDESITE"
    ));
    
    private static final Set<String> DIRT_MATERIALS = new HashSet<>(Arrays.asList(
        "DIRT", "GRASS_BLOCK", "GRASS", "PODZOL", "MYCELIUM", "COARSE_DIRT"
    ));
    
    private static final Set<String> WOOD_MATERIALS = new HashSet<>(Arrays.asList(
        "LOG", "WOOD", "OAK_LOG", "BIRCH_LOG", "SPRUCE_LOG", "JUNGLE_LOG",
        "ACACIA_LOG", "DARK_OAK_LOG", "OAK_WOOD", "BIRCH_WOOD", "SPRUCE_WOOD",
        "JUNGLE_WOOD", "ACACIA_WOOD", "DARK_OAK_WOOD"
    ));
    
    private static final Set<String> DIAMOND_ORE_MATERIALS = new HashSet<>(Arrays.asList(
        "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE"
    ));
    
    public static boolean isStone(Material material) {
        return STONE_MATERIALS.contains(material.name());
    }
    
    public static boolean isDirt(Material material) {
        return DIRT_MATERIALS.contains(material.name());
    }
    
    public static boolean isWood(Material material) {
        return WOOD_MATERIALS.contains(material.name());
    }
    
    public static boolean isDiamondOre(Material material) {
        return DIAMOND_ORE_MATERIALS.contains(material.name());
    }
    
    public static boolean isAir(Material material) {
        String name = material.name();
        return name.equals("AIR") || name.equals("CAVE_AIR") || name.equals("VOID_AIR");
    }
    
    public static boolean isWater(Material material) {
        String name = material.name();
        return name.equals("WATER") || name.equals("STATIONARY_WATER");
    }
    
    public static boolean isLava(Material material) {
        String name = material.name();
        return name.equals("LAVA") || name.equals("STATIONARY_LAVA");
    }
    
    // Safe material getting with fallbacks
    public static Material getMaterial(String... names) {
        for (String name : names) {
            try {
                return Material.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // Try next name
            }
        }
        return Material.STONE; // Default fallback
    }
}
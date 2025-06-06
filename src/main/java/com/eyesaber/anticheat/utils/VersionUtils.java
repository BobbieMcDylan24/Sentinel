package com.eyesaber.anticheat.utils;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

public class VersionUtils {
    
    private static final String VERSION = Bukkit.getServer().getClass().getPackage().getName();
    private static final int MAJOR_VERSION = getMajorVersion();
    private static final int MINOR_VERSION = getMinorVersion();
    
    public static int getMajorVersion() {
        try {
            String version = Bukkit.getBukkitVersion();
            String[] parts = version.split("\\.");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 20; // Default to 1.20
        }
    }
    
    public static int getMinorVersion() {
        try {
            String version = Bukkit.getBukkitVersion();
            String[] parts = version.split("\\.");
            if (parts.length > 2) {
                String minorPart = parts[2].split("-")[0];
                return Integer.parseInt(minorPart);
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public static String getVersion() {
        return Bukkit.getBukkitVersion();
    }
    
    public static boolean isVersionOrHigher(int majorVersion) {
        return MAJOR_VERSION >= majorVersion;
    }
    
    public static boolean isVersionOrHigher(int majorVersion, int minorVersion) {
        return MAJOR_VERSION > majorVersion || 
               (MAJOR_VERSION == majorVersion && MINOR_VERSION >= minorVersion);
    }
    
    public static boolean hasElytra() {
        return isVersionOrHigher(9);
    }
    
    public static boolean hasOffHand() {
        return isVersionOrHigher(9);
    }
    
    public static boolean hasNewCombatMechanics() {
        return isVersionOrHigher(9);
    }
    
    public static boolean hasNewMaterials() {
        return isVersionOrHigher(13);
    }
    
    public static boolean hasNewPotionEffects() {
        return isVersionOrHigher(13);
    }
    
    // Safe enchantment getting
    public static Enchantment getEfficiencyEnchantment() {
        try {
            // Try modern name first (1.13+)
            return Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("efficiency"));
        } catch (Exception e) {
            try {
                // Try legacy name
                return Enchantment.getByName("DIG_SPEED");
            } catch (Exception ex) {
                // For very old versions, try to find efficiency by iterating (without deprecated getName)
                for (Enchantment ench : Enchantment.values()) {
                    if (ench.getKey().getKey().equals("efficiency") || 
                        ench.getKey().getKey().equals("dig_speed")) {
                        return ench;
                    }
                }
                return null;
            }
        }
    }
    
    // Safe potion effect getting
    public static PotionEffectType getHasteEffect() {
        try {
            // Try modern name first (1.20.5+)
            return PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft("haste"));
        } catch (Exception e) {
            try {
                // Try legacy name
                return PotionEffectType.getByName("FAST_DIGGING");
            } catch (Exception ex) {
                try {
                    // Try direct field access
                    return (PotionEffectType) PotionEffectType.class.getField("HASTE").get(null);
                } catch (Exception exx) {
                    try {
                        return (PotionEffectType) PotionEffectType.class.getField("FAST_DIGGING").get(null);
                    } catch (Exception exxx) {
                        // Find by iterating through all effects
                        for (PotionEffectType effect : PotionEffectType.values()) {
                            if (effect != null && (effect.getKey().getKey().equals("haste") || 
                                effect.getKey().getKey().equals("fast_digging"))) {
                                return effect;
                            }
                        }
                        return null;
                    }
                }
            }
        }
    }
    
    public static String getServerVersion() {
        return VERSION;
    }
    
    public static int getServerMajorVersion() {
        return MAJOR_VERSION;
    }
    
    public static int getServerMinorVersion() {
        return MINOR_VERSION;
    }
}
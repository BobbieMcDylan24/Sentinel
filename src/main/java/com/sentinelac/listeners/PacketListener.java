package com.sentinelac.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.sentinelac.SentinelAC;
import com.sentinelac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class PacketListener {

    private final SentinelAC plugin;
    private ProtocolManager protocolManager;
    private PacketAdapter incomingAdapter;
    private PacketAdapter outgoingAdapter;

    public PacketListener(SentinelAC plugin) {
        this.plugin = plugin;
    }
    
    public void initialize() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            plugin.getLogger().severe("ProtocolLib is not installed! Packet listening is disabled.");
            return;
        }
        
        protocolManager = ProtocolLibrary.getProtocolManager();
        
        plugin.getLogger().info("Initializing packet listeners...");
        
        setupIncomingPacketListener();
        setupOutgoingPacketListener();
        
        plugin.getLogger().info("Packet listeners initialized successfully");
    }
    
    private void setupIncomingPacketListener() {
        incomingAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.FLYING,
                PacketType.Play.Client.ARM_ANIMATION,
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.BLOCK_DIG,
                PacketType.Play.Client.BLOCK_PLACE,
                PacketType.Play.Client.HELD_ITEM_SLOT) {
            
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                
                try {
                    handleIncomingPacket(event);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error handling incoming packet from " + 
                        event.getPlayer().getName(), e);
                }
            }
        };
        
        protocolManager.addPacketListener(incomingAdapter);
    }
    
    private void setupOutgoingPacketListener() {
        outgoingAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.POSITION,
                PacketType.Play.Server.ENTITY_VELOCITY,
                PacketType.Play.Server.ENTITY_TELEPORT) {
            
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                
                try {
                    handleOutgoingPacket(event);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error handling outgoing packet to " + 
                        event.getPlayer().getName(), e);
                }
            }
        };
        
        protocolManager.addPacketListener(outgoingAdapter);
    }
    
    private void handleIncomingPacket(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        
        if (data == null || data.isExempt()) {
            return;
        }
        
        // Update packet timing
        data.updatePacketTime();
        
        PacketType packetType = event.getPacketType();
        
        // Handle movement packets
        if (isMovementPacket(packetType)) {
            handleMovementPacket(event, player, data);
        }
        
        // Handle combat packets
        if (isCombatPacket(packetType)) {
            handleCombatPacket(event, player, data);
        }
        
        // Handle interaction packets
        if (isInteractionPacket(packetType)) {
            handleInteractionPacket(event, player, data);
        }
        
        // Run packet-specific checks
        plugin.getCheckManager().runPacketChecks(player, data, event.getPacket());
    }
    
    private void handleOutgoingPacket(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        
        if (data == null) {
            return;
        }
        
        PacketType packetType = event.getPacketType();
        
        // Handle server-side movement packets (teleports, velocity changes)
        if (packetType == PacketType.Play.Server.POSITION) {
            // Player is being teleported by server
            data.setTemporaryExempt(1000); // 1 second exemption after teleport
        } else if (packetType == PacketType.Play.Server.ENTITY_VELOCITY) {
            // Player velocity is being modified
            data.setTemporaryExempt(500); // 0.5 second exemption after velocity change
        }
    }
    
    private void handleMovementPacket(PacketEvent event, Player player, PlayerData data) {
        PacketType packetType = event.getPacketType();
        
        try {
            // Extract position data from packet
            double x = 0, y = 0, z = 0;
            float yaw = 0, pitch = 0;
            boolean hasPosition = false, hasRotation = false;
            
            if (packetType == PacketType.Play.Client.POSITION || 
                packetType == PacketType.Play.Client.POSITION_LOOK) {
                x = event.getPacket().getDoubles().read(0);
                y = event.getPacket().getDoubles().read(1);
                z = event.getPacket().getDoubles().read(2);
                hasPosition = true;
            }
            
            if (packetType == PacketType.Play.Client.LOOK || 
                packetType == PacketType.Play.Client.POSITION_LOOK) {
                yaw = event.getPacket().getFloat().read(0);
                pitch = event.getPacket().getFloat().read(1);
                hasRotation = true;
            }
            
            // Update player data with new location
            if (hasPosition) {
                Location newLocation = new Location(player.getWorld(), x, y, z);
                if (hasRotation) {
                    newLocation.setYaw(yaw);
                    newLocation.setPitch(pitch);
                } else {
                    newLocation.setYaw(player.getLocation().getYaw());
                    newLocation.setPitch(player.getLocation().getPitch());
                }
                
                data.updateLocation(newLocation);
            }
            
            // Run movement checks
            plugin.getCheckManager().runMovementChecks(player, data);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error processing movement packet for " + player.getName(), e);
        }
    }
    
    private void handleCombatPacket(PacketEvent event, Player player, PlayerData data) {
        // Run combat checks
        plugin.getCheckManager().runCombatChecks(player, data, event.getPacket());
    }
    
    private void handleInteractionPacket(PacketEvent event, Player player, PlayerData data) {
        // Run interaction checks
        plugin.getCheckManager().runInteractionChecks(player, data, event.getPacket());
    }
    
    private boolean isMovementPacket(PacketType packetType) {
        return packetType == PacketType.Play.Client.POSITION ||
               packetType == PacketType.Play.Client.POSITION_LOOK ||
               packetType == PacketType.Play.Client.LOOK ||
               packetType == PacketType.Play.Client.FLYING;
    }
    
    private boolean isCombatPacket(PacketType packetType) {
        return packetType == PacketType.Play.Client.ARM_ANIMATION ||
               packetType == PacketType.Play.Client.USE_ENTITY;
    }
    
    private boolean isInteractionPacket(PacketType packetType) {
        return packetType == PacketType.Play.Client.BLOCK_DIG ||
               packetType == PacketType.Play.Client.BLOCK_PLACE ||
               packetType == PacketType.Play.Client.HELD_ITEM_SLOT;
    }
    
    public void shutdown() {
        plugin.getLogger().info("Shutting down packet listeners...");
        
        if (protocolManager != null) {
            if (incomingAdapter != null) {
                protocolManager.removePacketListener(incomingAdapter);
            }
            if (outgoingAdapter != null) {
                protocolManager.removePacketListener(outgoingAdapter);
            }
        }
        
        plugin.getLogger().info("Packet listeners shutdown complete");
    }
    
    // Utility methods for checks
    public static boolean isPlayerMoving(PacketEvent event) {
        PacketType type = event.getPacketType();
        return type == PacketType.Play.Client.POSITION || 
               type == PacketType.Play.Client.POSITION_LOOK;
    }
    
    public static boolean isPlayerRotating(PacketEvent event) {
        PacketType type = event.getPacketType();
        return type == PacketType.Play.Client.LOOK || 
               type == PacketType.Play.Client.POSITION_LOOK;
    }
    
    public static double getPacketDistance(PacketEvent event, Location from) {
        if (!isPlayerMoving(event) || from == null) {
            return 0.0;
        }
        
        try {
            double x = event.getPacket().getDoubles().read(0);
            double y = event.getPacket().getDoubles().read(1);
            double z = event.getPacket().getDoubles().read(2);
            
            double deltaX = x - from.getX();
            double deltaY = y - from.getY();
            double deltaZ = z - from.getZ();
            
            return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    public static double getHorizontalDistance(PacketEvent event, Location from) {
        if (!isPlayerMoving(event) || from == null) {
            return 0.0;
        }
        
        try {
            double x = event.getPacket().getDoubles().read(0);
            double z = event.getPacket().getDoubles().read(2);
            
            double deltaX = x - from.getX();
            double deltaZ = z - from.getZ();
            
            return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        } catch (Exception e) {
            return 0.0;
        }
    }
}

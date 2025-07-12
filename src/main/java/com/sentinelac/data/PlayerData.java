package com.sentinelac.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerData {

    private final UUID uuid;
    private final String playerName;
    private final long joinTime;
    private final AtomicLong lastSeen = new AtomicLong();
    
    // Connection data
    private volatile int ping = 0;
    private volatile double serverTPS = 20.0;
    
    // Movement data
    private volatile Location lastLocation;
    private volatile Location currentLocation;
    private final AtomicLong lastMovement = new AtomicLong();
    private volatile double lastMotionX = 0;
    private volatile double lastMotionY = 0;
    private volatile double lastMotionZ = 0;
    
    // Violation tracking
    private final ConcurrentHashMap<String, AtomicInteger> violations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastViolationTime = new ConcurrentHashMap<>();
    private final AtomicInteger totalViolations = new AtomicInteger(0);
    
    // Check-specific data
    private final ConcurrentHashMap<String, Object> checkData = new ConcurrentHashMap<>();
    
    // Lag compensation
    private volatile long lastPacketTime = System.currentTimeMillis();
    private volatile boolean lagging = false;
    
    // Exemptions
    private volatile boolean exempt = false;
    private volatile long exemptUntil = 0;

    public PlayerData(Player player) {
        this.uuid = player.getUniqueId();
        this.playerName = player.getName();
        this.joinTime = System.currentTimeMillis();
        this.lastSeen.set(joinTime);
        this.currentLocation = player.getLocation().clone();
        this.lastLocation = currentLocation.clone();
        this.lastMovement.set(joinTime);
        this.lastPacketTime = joinTime;
        
        // Check for exemption permission
        updateExemptStatus(player);
    }
    
    public void updateLocation(Location newLocation) {
        if (newLocation != null) {
            this.lastLocation = this.currentLocation;
            this.currentLocation = newLocation.clone();
            this.lastMovement.set(System.currentTimeMillis());
            updateLastSeen();
        }
    }
    
    public void updateMotion(double motionX, double motionY, double motionZ) {
        this.lastMotionX = motionX;
        this.lastMotionY = motionY;
        this.lastMotionZ = motionZ;
        updateLastSeen();
    }
    
    public void updateLastSeen() {
        this.lastSeen.set(System.currentTimeMillis());
    }
    
    public void updatePacketTime() {
        this.lastPacketTime = System.currentTimeMillis();
        updateLastSeen();
    }
    
    public void updateExemptStatus(Player player) {
        this.exempt = player.hasPermission("sentinelac.exempt") || player.isOp();
    }
    
    public void setTemporaryExempt(long durationMs) {
        this.exemptUntil = System.currentTimeMillis() + durationMs;
    }
    
    public boolean isExempt() {
        if (exempt) return true;
        if (exemptUntil > 0 && System.currentTimeMillis() < exemptUntil) return true;
        if (exemptUntil > 0 && System.currentTimeMillis() >= exemptUntil) {
            exemptUntil = 0; // Reset temporary exemption
        }
        return false;
    }
    
    // Violation methods
    public int addViolation(String checkName) {
        AtomicInteger violations = this.violations.computeIfAbsent(checkName, k -> new AtomicInteger(0));
        int newViolations = violations.incrementAndGet();
        totalViolations.incrementAndGet();
        lastViolationTime.put(checkName, new AtomicLong(System.currentTimeMillis()));
        return newViolations;
    }
    
    public int getViolations(String checkName) {
        AtomicInteger violations = this.violations.get(checkName);
        return violations != null ? violations.get() : 0;
    }
    
    public void resetViolations(String checkName) {
        violations.remove(checkName);
        lastViolationTime.remove(checkName);
    }
    
    public void resetAllViolations() {
        violations.clear();
        lastViolationTime.clear();
        totalViolations.set(0);
    }
    
    public long getLastViolationTime(String checkName) {
        AtomicLong time = lastViolationTime.get(checkName);
        return time != null ? time.get() : 0;
    }
    
    // Check data methods
    public void setCheckData(String key, Object value) {
        if (value == null) {
            checkData.remove(key);
        } else {
            checkData.put(key, value);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getCheckData(String key, Class<T> type) {
        Object value = checkData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    public boolean hasCheckData(String key) {
        return checkData.containsKey(key);
    }
    
    // Lag detection
    public void updateLagStatus() {
        long timeSinceLastPacket = System.currentTimeMillis() - lastPacketTime;
        this.lagging = timeSinceLastPacket > 150 || ping > 200 || serverTPS < 18.0;
    }
    
    // Getters
    public UUID getUUID() {
        return uuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
    
    public boolean isOnline() {
        Player player = getPlayer();
        return player != null && player.isOnline();
    }
    
    public long getJoinTime() {
        return joinTime;
    }
    
    public long getLastSeen() {
        return lastSeen.get();
    }
    
    public int getPing() {
        return ping;
    }
    
    public void setPing(int ping) {
        this.ping = ping;
        updateLagStatus();
    }
    
    public double getServerTPS() {
        return serverTPS;
    }
    
    public void setServerTPS(double serverTPS) {
        this.serverTPS = serverTPS;
        updateLagStatus();
    }
    
    public Location getCurrentLocation() {
        return currentLocation != null ? currentLocation.clone() : null;
    }
    
    public Location getLastLocation() {
        return lastLocation != null ? lastLocation.clone() : null;
    }
    
    public long getLastMovement() {
        return lastMovement.get();
    }
    
    public double getLastMotionX() {
        return lastMotionX;
    }
    
    public double getLastMotionY() {
        return lastMotionY;
    }
    
    public double getLastMotionZ() {
        return lastMotionZ;
    }
    
    public int getTotalViolations() {
        return totalViolations.get();
    }
    
    public boolean isLagging() {
        updateLagStatus();
        return lagging;
    }
    
    public long getLastPacketTime() {
        return lastPacketTime;
    }
    
    // Utility methods
    public double getDistanceMoved() {
        if (lastLocation == null || currentLocation == null) {
            return 0.0;
        }
        
        if (!lastLocation.getWorld().equals(currentLocation.getWorld())) {
            return 0.0;
        }
        
        return lastLocation.distance(currentLocation);
    }
    
    public double getHorizontalDistance() {
        if (lastLocation == null || currentLocation == null) {
            return 0.0;
        }
        
        if (!lastLocation.getWorld().equals(currentLocation.getWorld())) {
            return 0.0;
        }
        
        double deltaX = currentLocation.getX() - lastLocation.getX();
        double deltaZ = currentLocation.getZ() - lastLocation.getZ();
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
    
    public double getVerticalDistance() {
        if (lastLocation == null || currentLocation == null) {
            return 0.0;
        }
        
        if (!lastLocation.getWorld().equals(currentLocation.getWorld())) {
            return 0.0;
        }
        
        return currentLocation.getY() - lastLocation.getY();
    }
    
    public long getTimeSinceLastMovement() {
        return System.currentTimeMillis() - lastMovement.get();
    }
    
    public long getSessionDuration() {
        return System.currentTimeMillis() - joinTime;
    }
    
    public void cleanup() {
        violations.clear();
        lastViolationTime.clear();
        checkData.clear();
    }
    
    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", playerName='" + playerName + '\'' +
                ", ping=" + ping +
                ", totalViolations=" + totalViolations +
                ", lagging=" + lagging +
                ", exempt=" + isExempt() +
                '}';
    }
}

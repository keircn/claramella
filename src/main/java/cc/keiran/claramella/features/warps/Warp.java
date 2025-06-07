package cc.keiran.claramella.features.warps;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;

import java.util.UUID;

public class Warp {
    
    private final String name;
    private final UUID worldId;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final UUID createdBy;
    private final long createdAt;
    
    public Warp(String name, Location location, UUID createdBy) {
        this.name = name;
        this.worldId = location.getWorld().getUID();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
    }
    
    public Warp(String name, UUID worldId, double x, double y, double z, float yaw, float pitch, UUID createdBy, long createdAt) {
        this.name = name;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
    
    public String getName() {
        return name;
    }
    
    public UUID getWorldId() {
        return worldId;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getZ() {
        return z;
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public Location getLocation() {
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    public boolean isWorldLoaded() {
        return Bukkit.getWorld(worldId) != null;
    }
}

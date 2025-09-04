package engine.client.common;

import java.util.UUID;

import com.esotericsoftware.kryonet.Connection;

import server.Server;
import server.event.player.PlayerChatEvent;

public class Player {
    private float x,y,z;
    private float yaw, pitch;
    private final UUID uuid;
    private String name;
    public Player(UUID uuid, String name) { 
    	x=0; y=5; z=0;
    	this.uuid = uuid;
    	this.name = name;
    }
    public UUID getUniqueID() { return uuid; }
    public String getDisplayName() { return name; }
    public void setDisplayName(String name) { this.name = name;}
    public void setPosition(float x, float y, float z) { this.x=x; this.y=y; this.z=z; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float[] getPosition() { return new float[]{x,y,z}; }
}
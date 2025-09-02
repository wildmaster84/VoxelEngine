package engine.common.player;

import engine.client.WorldView;
import engine.common.block.Block;
import engine.common.world.Chunk;
import engine.server.World;

public class Player {
    private float x,y,z;
    private float yaw, pitch;
    public Player() { x=0; y=5; z=0; }
    public void setPosition(float x, float y, float z) { this.x=x; this.y=y; this.z=z; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float[] getPosition() { return new float[]{x,y,z}; }
 // Physics fields
    private float velocityY = 0f;
    private boolean onGround = false;

    // Constants
    private static final float GRAVITY = -0.07f;
    private static final float TERMINAL_VELOCITY = -3f;
    private static final float PLAYER_HEIGHT = 1.8f; // Example
    
    public void updatePhysics(World world) {
    	System.out.println("Location: " + x + ", " + y + ", " + z);
        if (!onGround) {
            velocityY += GRAVITY;
            if (velocityY < TERMINAL_VELOCITY) velocityY = TERMINAL_VELOCITY;
        }

        float nextY = y + velocityY;

        // Simple ground check: see if block below player is solid
        int blockX = (int)x;
        int blockY = (int)(nextY + 1.0f);
        int blockZ = (int)z;
        Chunk chunk = world.getChunk(blockX >> 4, 0, blockZ >> 4);
        if (chunk != null) {
            Block below = chunk.getBlock(blockX & 15, blockY, blockZ & 15);
            if (below != null && below.getType() != 0) {
                // Land on ground
                onGround = true;
                velocityY = 0;
                y = blockY + 1;
            } else {
                onGround = false;
                y = nextY;
            }
        } else {
            onGround = false;
            y = nextY;
        }
    }
}
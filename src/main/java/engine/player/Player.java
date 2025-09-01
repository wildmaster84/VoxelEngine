package engine.player;

public class Player {
    private float x,y,z;
    private float yaw, pitch;
    public Player() { x=0; y=64; z=0; }
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
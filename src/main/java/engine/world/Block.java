package engine.world;

public class Block {
    private byte type;
    public Block(byte type) { this.type = type; }
    public byte getType() { return type; }
    public void setType(byte type) { this.type = type; }
}
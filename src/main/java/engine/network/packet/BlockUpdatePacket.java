package engine.network.packet;
public class BlockUpdatePacket {
    public int chunkX, chunkY, chunkZ;
    public int x, y, z;
    public byte blockType;
}
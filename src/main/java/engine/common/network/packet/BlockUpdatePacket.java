package engine.common.network.packet;

import engine.common.block.Material;

public class BlockUpdatePacket {
    public int chunkX, chunkY, chunkZ;
    public int x, y, z;
    public Material blockType;
}
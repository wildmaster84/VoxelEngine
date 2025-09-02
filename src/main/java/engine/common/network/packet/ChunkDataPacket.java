package engine.common.network.packet;

public class ChunkDataPacket {
    public int chunkX, chunkY, chunkZ;
    public byte[] blockTypes; // size = Chunk.SIZE * Chunk.SIZE * Chunk.SIZE

    public ChunkDataPacket() {}

    public ChunkDataPacket(int chunkX, int chunkY, int chunkZ, byte[] blockTypes) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.blockTypes = blockTypes;
    }
}
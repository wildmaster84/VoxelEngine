package engine.client;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import engine.common.block.Block;
import engine.common.world.Chunk;

public class WorldView {
    private Map<String, Chunk> chunks = new ConcurrentHashMap<>();

    public void setChunk(Chunk chunk) {
        String key = chunk.getX() + "," + chunk.getY() + "," + chunk.getZ();
        chunks.put(key, chunk);
    }
    public Chunk getChunk(int x, int y, int z) {
        String key = x + "," + y + "," + z;
        return chunks.get(key);
    }
    // Other view-only methods; NO generation or saving
	public Collection<Chunk> getChunks() {
		// TODO Auto-generated method stub
		return chunks.values();
	}
	public Block getBlock(int x, int y, int z) {
	    int chunkX = x >> 4;
	    int chunkY = y >> 4;
	    int chunkZ = z >> 4;
	    Chunk chunk = getChunk(chunkX, chunkY, chunkZ);
	    if (chunk == null) return null;
	    int localX = x & 15;
	    int localY = y & 15;
	    int localZ = z & 15;
	    return chunk.getBlock(localX, localY, localZ);
	}
}
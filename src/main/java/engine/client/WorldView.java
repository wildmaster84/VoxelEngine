package engine.client;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
}
package engine.world;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class World {
    public final Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    private final File worldDir;
    public World(File worldDir) {
        this.worldDir = worldDir;
        if(!worldDir.exists()) worldDir.mkdirs();
    }
    private String key(int x,int y,int z){ return x + "," + y + "," + z; }
    public Chunk getChunk(int x, int y, int z) {
        return chunks.get(key(x, y, z));
    }
    public String getChunkKey(int x, int y, int z) {
        return key(x, y, z);
    }
    public Collection<Chunk> getChunks() {
        return chunks.values();
    }
    public void setChunk(Chunk chunk) {
        chunks.put(key(chunk.getX(), chunk.getY(), chunk.getZ()), chunk);
    }
    public void saveAll() {
        for(Chunk chunk : chunks.values()) {
            saveChunk(chunk);
        }
    }
    public void saveChunk(Chunk chunk) {
    	File f = new File(worldDir, "chunk_" + chunk.getX() + "_" + chunk.getY() + "_" + chunk.getZ() + ".bin");
        try {
			chunk.save(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    public void loadAll() throws IOException {
        File[] files = worldDir.listFiles((dir, name) -> name.startsWith("chunk_") && name.endsWith(".bin"));
        if(files == null) return;
        for(File f : files) {
            Chunk chunk = Chunk.load(f);
            setChunk(chunk);
        }
    }
    public Chunk getOrCreateChunk(int x,int y,int z) {
        Chunk c = getChunk(x, y, z);
        if(c == null) {
            c = new Chunk(x, y, z);
            setChunk(c);
        }
        return c;
    }
}
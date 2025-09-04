package engine.server;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import engine.common.world.AsyncChunkGenerator;
import engine.common.world.Chunk;
import engine.common.block.BlockRegistry;

public class World {
    public final Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    private final File worldDir;
    private final AsyncChunkGenerator asyncChunkGenerator = new AsyncChunkGenerator();
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
    // Pass chunkGenerator and blockRegistry as parameters!
    public Chunk getOrCreateChunk(int x, int y, int z, ChunkGenerator chunkGenerator, BlockRegistry blockRegistry) {
        Chunk chunk = getChunk(x, y, z);
        if (chunk == null) {
            chunk = Chunk.createGenerated(x, y, z); // Generate chunk (server only)
            chunkGenerator.generate(chunk, blockRegistry);
            setChunk(chunk);
            saveChunk(chunk);
        }
        return chunk;
    }
    
    public CompletableFuture<Chunk> getOrCreateChunkAsync(int x, int y, int z, ChunkGenerator chunkGenerator, BlockRegistry blockRegistry) {
        Chunk chunk = getChunk(x, y, z);
        if (chunk != null) {
            return CompletableFuture.completedFuture(chunk);
        } else {
            CompletableFuture<Chunk> future = asyncChunkGenerator.generateChunkAsync(x, y, z, chunkGenerator, blockRegistry);
            future.thenAccept(genChunk -> {
                setChunk(genChunk);
                saveChunk(genChunk);
            });
            return future;
        }
    }

    public void shutdownAsyncGenerator() {
        asyncChunkGenerator.shutdown();
    }
}
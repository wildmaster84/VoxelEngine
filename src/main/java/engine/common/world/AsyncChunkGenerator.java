package engine.common.world;

import engine.common.block.BlockRegistry;
import engine.common.world.Chunk;
import engine.server.ChunkGenerator;

import java.util.concurrent.*;

public class AsyncChunkGenerator {
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public CompletableFuture<Chunk> generateChunkAsync(int x, int y, int z, ChunkGenerator generator) {
        CompletableFuture<Chunk> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                Chunk chunk = Chunk.createGenerated(x, y, z);
                generator.generate(chunk);
                future.complete(chunk);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
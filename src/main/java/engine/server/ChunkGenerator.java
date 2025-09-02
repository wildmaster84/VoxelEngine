package engine.server;

import engine.common.block.BlockRegistry;
import engine.common.world.Chunk;

public interface ChunkGenerator {
    void generate(Chunk chunk, BlockRegistry registry);
}
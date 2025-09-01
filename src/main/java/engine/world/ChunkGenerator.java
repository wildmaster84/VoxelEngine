package engine.world;

import engine.block.BlockRegistry;

public interface ChunkGenerator {
    void generate(Chunk chunk, BlockRegistry registry);
}
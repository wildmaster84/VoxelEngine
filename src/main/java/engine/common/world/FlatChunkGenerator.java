package engine.common.world;

import engine.common.block.Block;
import engine.common.block.BlockRegistry;
import engine.server.ChunkGenerator;

public class FlatChunkGenerator implements ChunkGenerator {
    @Override
    public void generate(Chunk chunk, BlockRegistry registry) {
        byte grass = registry.getInfo("grass").id;
        byte dirt = registry.getInfo("dirt").id;
        for(int x=0;x<Chunk.SIZE;x++)
            for(int z=0;z<Chunk.SIZE;z++)
                for(int y=0;y<Chunk.SIZE;y++) {
                    if(y == 4)
                        chunk.setBlock(x, y, z, new Block(grass));
                    else if(y < 4)
                        chunk.setBlock(x, y, z, new Block(dirt));
                }
    }
}
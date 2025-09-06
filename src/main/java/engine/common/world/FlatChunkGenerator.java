package engine.common.world;

import java.util.Random;

import engine.common.block.Block;
import engine.common.block.BlockRegistry;
import engine.common.block.Material;
import engine.server.ChunkGenerator;

public class FlatChunkGenerator implements ChunkGenerator {
    @Override
    public void generate(Chunk chunk) {
    	int waterLevel = 3;
        for(int x=0;x<Chunk.SIZE;x++)
            for(int z=0;z<Chunk.SIZE;z++)
                for(int y=0;y<Chunk.SIZE;y++) {                	
                    if(y == 4) {
                    	Random random = new Random(1);
                    	
                    	if (random.nextInt() == 1)
                    		chunk.setBlock(x, y, z, new Block(Material.GRASS));
                    	else chunk.setBlock(x, y, z, new Block(Material.WATER));
                    
                    }
                    else if(y < waterLevel)
                        chunk.setBlock(x, y, z, new Block(Material.WATER));
                    else if(y < 4)
                        chunk.setBlock(x, y, z, new Block(Material.DIRT));
                }
    }
}
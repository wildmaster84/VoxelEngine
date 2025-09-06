package engine.common.world;

import engine.common.block.Block;
import engine.common.block.Material;
import engine.server.ChunkGenerator;

// Simple demo "noise" function. Replace with Perlin/OpenSimplex for production!
class FakeNoise {
    public static double noise(double x, double z) {
        // Generates hills and valleys, but NOT just stone
        return Math.sin(x * 0.012) * 12 + Math.cos(z * 0.018) * 9;
    }
}

public class DefaultChunkGenerator implements ChunkGenerator {
    private final int seaLevel;
    private final int maxHeight;

    public DefaultChunkGenerator(int seaLevel, int maxHeight) {
        this.seaLevel = seaLevel;
        this.maxHeight = maxHeight;
    }

    @Override
    public void generate(Chunk chunk) {
        int baseX = chunk.getX() * Chunk.SIZE;
        int baseY = chunk.getY() * Chunk.SIZE;
        int baseZ = chunk.getZ() * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;

                // Terrain height based on noise (varied, not flat)
                int terrainHeight = seaLevel + (int)FakeNoise.noise(worldX, worldZ);
                terrainHeight = Math.max(8, Math.min(maxHeight, terrainHeight));

                for (int y = 0; y < Chunk.SIZE; y++) {
                    int worldY = baseY + y;
                    Block block;

                    if (worldY > terrainHeight) {
                        // Air or water above terrain
                        block = (worldY <= seaLevel) ? new Block(Material.WATER) : new Block(Material.AIR);
                    } else if (worldY == terrainHeight) {
                        // Surface block
                        if (terrainHeight < seaLevel + 2) {
                            block = new Block(Material.SAND); // Beach
                        } else if (terrainHeight > maxHeight - 10) {
                            block = new Block(Material.GRASS); // Mountain peak
                        } else {
                            block = new Block(Material.GRASS); // Grass
                        }
                    } else if (worldY > terrainHeight - 3) {
                        block = new Block(Material.DIRT); // Dirt under surface
                    } else {
                        block = new Block(Material.STONE); // Stone below
                    }

                    chunk.setBlock(x, y, z, block);
                }
            }
        }
    }
}
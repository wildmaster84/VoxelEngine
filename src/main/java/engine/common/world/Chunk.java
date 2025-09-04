package engine.common.world;

import java.io.*;

import engine.common.block.Block;
import engine.common.block.Material;
import engine.common.network.NetworkManager;

/**
 * Shared chunk class. Only the server should generate/fill blocks.
 * The client should only fill blocks via deserializeBlocks, using the factory method below.
 */
public class Chunk {
    public static final int SIZE = 16;
    private final int chunkX, chunkY, chunkZ;
    private final Block[][][] blocks = new Block[SIZE][SIZE][SIZE];

    // Private constructor: does NOT fill blocks!
    private Chunk(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
    }

    /** Server-side factory: generates blocks as empty (air/0) and then generator fills them. */
    public static Chunk createGenerated(int chunkX, int chunkY, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkY, chunkZ);
        for(int x=0;x<SIZE;x++)
            for(int y=0;y<SIZE;y++)
                for(int z=0;z<SIZE;z++)
                    chunk.blocks[x][y][z] = new Block(Material.AIR);
        return chunk;
    }

    /** Client-side factory: creates chunk and immediately fills from network data. */
    public static Chunk fromNetwork(int chunkX, int chunkY, int chunkZ, byte[] packet) {
        Chunk chunk = new Chunk(chunkX, chunkY, chunkZ);
        chunk.deserializeBlocks(packet);
        return chunk;
    }

    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return null;
        return blocks[x][y][z];
    }
    public void setBlock(int x, int y, int z, Block block) { blocks[x][y][z] = block; }
    public int getX() { return chunkX; }
    public int getY() { return chunkY; }
    public int getZ() { return chunkZ; }

    public void save(File file) throws IOException {
        try(DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeInt(chunkX); out.writeInt(chunkY); out.writeInt(chunkZ);
            for(int x=0;x<SIZE;x++)
                for(int y=0;y<SIZE;y++)
                    for(int z=0;z<SIZE;z++)
                    	out.writeByte(blocks[x][y][z].getType().getId());
        }
    }
    public static Chunk load(File file) throws IOException {
        try(DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int cx = in.readInt(), cy = in.readInt(), cz = in.readInt();
            Chunk chunk = createGenerated(cx, cy, cz); // Loads on server
            for(int x=0;x<SIZE;x++)
                for(int y=0;y<SIZE;y++)
                    for(int z=0;z<SIZE;z++)
                    	chunk.blocks[x][y][z].setType(Material.fromId(in.readByte()));
            return chunk;
        }
    }

    public byte[] serializeBlocks() {
    	byte[] data = new byte[SIZE * SIZE * SIZE];
    	int i = 0;
    	for (int x = 0; x < SIZE; x++)
    	    for (int y = 0; y < SIZE; y++)
    	        for (int z = 0; z < SIZE; z++)
    	            data[i++] = (byte)getBlock(x, y, z).getType().getId();
    	return NetworkManager.compress(data);
    }

    public void deserializeBlocks(byte[] packet) {
        byte[] data;
        try {
            data = NetworkManager.decompress(packet);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        int i = 0;
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                for (int z = 0; z < SIZE; z++) {
                	blocks[x][y][z] = new Block(Material.fromId(data[i++]));
                }
    }
}
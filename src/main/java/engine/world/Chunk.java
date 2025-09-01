package engine.world;

import java.io.*;

import engine.network.NetworkManager;

public class Chunk {
    public static final int SIZE = 16;
    private final int chunkX, chunkY, chunkZ;
    private final Block[][][] blocks = new Block[SIZE][SIZE][SIZE];
    public Chunk(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX; this.chunkY = chunkY; this.chunkZ = chunkZ;
        for(int x=0;x<SIZE;x++)
            for(int y=0;y<SIZE;y++)
                for(int z=0;z<SIZE;z++)
                    blocks[x][y][z] = new Block((byte)0);
    }
    public Block getBlock(int x, int y, int z) { return blocks[x][y][z]; }
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
                        out.writeByte(blocks[x][y][z].getType());
        }
    }
    public static Chunk load(File file) throws IOException {
        try(DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int cx = in.readInt(), cy = in.readInt(), cz = in.readInt();
            Chunk chunk = new Chunk(cx, cy, cz);
            for(int x=0;x<SIZE;x++)
                for(int y=0;y<SIZE;y++)
                    for(int z=0;z<SIZE;z++)
                        chunk.blocks[x][y][z].setType(in.readByte());
            return chunk;
        }
    }
 // Add these methods to your Chunk class

    public byte[] serializeBlocks() {
        byte[] data = new byte[SIZE * SIZE * SIZE];
        int i = 0;
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                for (int z = 0; z < SIZE; z++)
                    data[i++] = getBlock(x, y, z).getType(); // Adjust for your block storage
        return NetworkManager.compress(data);
    }

    public void deserializeBlocks(byte[] packet) {
    	byte[] data;
		try {
			data = NetworkManager.decompress(packet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
        int i = 0;
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                for (int z = 0; z < SIZE; z++) {
                	setBlock(x, y, z, new Block(data[i++])); // Adjust for your block constructor/storage
                }
                    
    }
}
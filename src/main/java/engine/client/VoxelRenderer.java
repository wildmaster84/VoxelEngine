package engine.client;

import engine.world.World;
import engine.world.Chunk;
import engine.block.BlockRegistry;
import engine.player.Player;

import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

public class VoxelRenderer {
    private World world;
    private BlockRegistry blockRegistry;
    private static final int VIEW_DISTANCE = 6;

    public VoxelRenderer(World world, BlockRegistry blockRegistry) {
        this.world = world;
        this.blockRegistry = blockRegistry;
    }

    public void renderWorld(Player player) {
        int playerChunkX = (int)Math.floor(player.getX() / Chunk.SIZE);
        int playerChunkY = (int)Math.floor(player.getY() / Chunk.SIZE);
        int playerChunkZ = (int)Math.floor(player.getZ() / Chunk.SIZE);

        for (Chunk chunk : world.getChunks()) {
            int dx = chunk.getX() - playerChunkX;
            int dy = chunk.getY() - playerChunkY;
            int dz = chunk.getZ() - playerChunkZ;
            if (Math.abs(dx) > VIEW_DISTANCE ||
                Math.abs(dy) > VIEW_DISTANCE ||
                Math.abs(dz) > VIEW_DISTANCE) {
            	world.saveChunk(chunk);
            	world.chunks.remove(world.getChunkKey(chunk.getX(), chunk.getY(), chunk.getZ()));
            	continue;
            }
            renderChunk(chunk);
        }
    }

    private void renderChunk(Chunk chunk) {
        int baseX = chunk.getX() * Chunk.SIZE;
        int baseY = chunk.getY() * Chunk.SIZE;
        int baseZ = chunk.getZ() * Chunk.SIZE;
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    byte type = chunk.getBlock(x, y, z).getType();
                    if (type != blockRegistry.getAirId()) {
                        if (shouldRenderFace(chunk, x, y, z, 0, 0, 1))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "front");
                        if (shouldRenderFace(chunk, x, y, z, 0, 0, -1))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "back");
                        if (shouldRenderFace(chunk, x, y, z, -1, 0, 0))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "left");
                        if (shouldRenderFace(chunk, x, y, z, 1, 0, 0))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "right");
                        if (shouldRenderFace(chunk, x, y, z, 0, 1, 0))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "top");
                        if (shouldRenderFace(chunk, x, y, z, 0, -1, 0))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "bottom");
                    }
                }
            }
        }
    }

    private boolean shouldRenderFace(Chunk chunk, int x, int y, int z, int dx, int dy, int dz) {
        int nx = x + dx, ny = y + dy, nz = z + dz;
        if (nx < 0 || nx >= Chunk.SIZE ||
            ny < 0 || ny >= Chunk.SIZE ||
            nz < 0 || nz >= Chunk.SIZE) {
            return true;
        }
        return chunk.getBlock(nx, ny, nz).getType() == blockRegistry.getAirId();
    }

    private void renderBlockFace(int x, int y, int z, byte type, String face) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        BlockRegistry.BlockInfo info = blockRegistry.getInfo(type);
        GL11.glColor3f(info.r, info.g, info.b);

        GL11.glBegin(GL11.GL_QUADS);
        switch (face) {
            case "front": GL11.glVertex3f(0,0,1); GL11.glVertex3f(1,0,1); GL11.glVertex3f(1,1,1); GL11.glVertex3f(0,1,1); break;
            case "back": GL11.glVertex3f(0,0,0); GL11.glVertex3f(1,0,0); GL11.glVertex3f(1,1,0); GL11.glVertex3f(0,1,0); break;
            case "left": GL11.glVertex3f(0,0,0); GL11.glVertex3f(0,0,1); GL11.glVertex3f(0,1,1); GL11.glVertex3f(0,1,0); break;
            case "right": GL11.glVertex3f(1,0,0); GL11.glVertex3f(1,0,1); GL11.glVertex3f(1,1,1); GL11.glVertex3f(1,1,0); break;
            case "top": GL11.glVertex3f(0,1,0); GL11.glVertex3f(1,1,0); GL11.glVertex3f(1,1,1); GL11.glVertex3f(0,1,1); break;
            case "bottom": GL11.glVertex3f(0,0,0); GL11.glVertex3f(1,0,0); GL11.glVertex3f(1,0,1); GL11.glVertex3f(0,0,1); break;
        }
        GL11.glEnd();
        GL11.glPopMatrix();
    }
}
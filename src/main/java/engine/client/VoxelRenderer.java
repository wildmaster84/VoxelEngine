package engine.client;

import engine.common.block.BlockRegistry;
import engine.common.player.Player;
import engine.common.world.Chunk;

import org.lwjgl.opengl.GL11;

public class VoxelRenderer {
    private WorldView world;
    private BlockRegistry blockRegistry;
    private TextureManager textureManager; // New field for texture system
    private static final int VIEW_DISTANCE = 6;

    public VoxelRenderer(WorldView world, BlockRegistry blockRegistry, TextureManager textureManager) {
        this.world = world;
        this.blockRegistry = blockRegistry;
        this.textureManager = textureManager;
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

    // Updated to use the texture manager
    private void renderBlockFace(int x, int y, int z, byte type, String face) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        BlockRegistry.BlockInfo info = blockRegistry.getInfo(type);

        //GL11.glColor3f(info.r, info.g, info.b);

        // Select the correct texture for the face
        String textureName;
        switch (face) {
            case "top":    textureName = info.textureTop;    break;
            case "bottom": textureName = info.textureBottom; break;
            default:       textureName = info.textureSide;   break; // front, back, left, right
        }
        if (type == blockRegistry.getAirId() || textureName == null) return;
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        textureManager.bindTexture(textureName);

        GL11.glBegin(GL11.GL_QUADS);
        switch (face) {
        case "front":
            GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0,0,1);
            GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1,0,1);
            GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1,1,1);
            GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0,1,1);
            break;
        case "back":
            GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0,0,0);
            GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1,0,0);
            GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1,1,0);
            GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0,1,0);
            break;
        case "left":
            GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0,0,0);
            GL11.glTexCoord2f(1, 1); GL11.glVertex3f(0,0,1);
            GL11.glTexCoord2f(1, 0); GL11.glVertex3f(0,1,1);
            GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0,1,0);
            break;
        case "right":
            GL11.glTexCoord2f(0, 1); GL11.glVertex3f(1,0,0);
            GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1,0,1);
            GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1,1,1);
            GL11.glTexCoord2f(0, 0); GL11.glVertex3f(1,1,0);
            break;
        case "top":
            GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0,1,0);
            GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1,1,0);
            GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1,1,1);
            GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0,1,1);
            break;
        case "bottom":
            GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0,0,0);
            GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1,0,0);
            GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1,0,1);
            GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0,0,1);
            break;
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }
}
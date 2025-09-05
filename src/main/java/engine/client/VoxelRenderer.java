package engine.client;

import engine.client.common.Player;
import engine.common.block.BlockRegistry;
import engine.common.block.Material;
import engine.common.world.Chunk;
import org.lwjgl.opengl.GL11;

public class VoxelRenderer {
    private WorldView world;
    private BlockRegistry blockRegistry;
    private TextureManager textureManager;
    private static final int VIEW_DISTANCE = 2;

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
            renderChunk(chunk, player);
        }
    }

    private void renderChunk(Chunk chunk, Player player) {
        int baseX = chunk.getX() * Chunk.SIZE;
        int baseY = chunk.getY() * Chunk.SIZE;
        int baseZ = chunk.getZ() * Chunk.SIZE;
        float px = player.getX();
        float py = player.getY();
        float pz = player.getZ();

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    Material type = chunk.getBlock(x, y, z).getType();
                    if (type != Material.AIR) {
                        // For each face, check not only if it's exposed, but if it's visible to the player
                        if (shouldRenderFace(chunk, x, y, z, 0, 0, 1) && isFaceVisibleToPlayer(px, py, pz, baseX + x, baseY + y, baseZ + z, "front", player))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "front");
                        if (shouldRenderFace(chunk, x, y, z, 0, 0, -1) && isFaceVisibleToPlayer(px, py, pz, baseX + x, baseY + y, baseZ + z, "back", player))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "back");
                        if (shouldRenderFace(chunk, x, y, z, -1, 0, 0) && isFaceVisibleToPlayer(px, py, pz, baseX + x, baseY + y, baseZ + z, "left", player))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "left");
                        if (shouldRenderFace(chunk, x, y, z, 1, 0, 0) && isFaceVisibleToPlayer(px, py, pz, baseX + x, baseY + y, baseZ + z, "right", player))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "right");
                        if (shouldRenderFace(chunk, x, y, z, 0, 1, 0) && isFaceVisibleToPlayer(px, py, pz, baseX + x, baseY + y, baseZ + z, "top", player))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "top");
                        if (shouldRenderFace(chunk, x, y, z, 0, -1, 0) && isFaceVisibleToPlayer(px, py, pz, baseX + x, baseY + y, baseZ + z, "bottom", player))
                            renderBlockFace(baseX + x, baseY + y, baseZ + z, type, "bottom");
                    }
                }
            }
        }
    }

    // Basic face culling: only render faces adjacent to air/outside
    private boolean shouldRenderFace(Chunk chunk, int x, int y, int z, int dx, int dy, int dz) {
        int nx = x + dx, ny = y + dy, nz = z + dz;
        if (nx < 0 || nx >= Chunk.SIZE ||
            ny < 0 || ny >= Chunk.SIZE ||
            nz < 0 || nz >= Chunk.SIZE) {
            return true;
        }
        return chunk.getBlock(nx, ny, nz).getType() == Material.AIR;
    }

    // Checks if a face is visible to the player (simple dot product with player look direction)
    private boolean isFaceVisibleToPlayer(float px, float py, float pz, int bx, int by, int bz, String face, Player player) {
        // Get player's look direction
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        float yawRad = (float)Math.toRadians(yaw);
        float pitchRad = (float)Math.toRadians(pitch);

        float lookX = (float)(Math.cos(pitchRad) * Math.sin(yawRad));
        float lookY = (float)(Math.sin(pitchRad));
        float lookZ = (float)(Math.cos(pitchRad) * Math.cos(yawRad));

        // Get face normal
        float nx = 0, ny = 0, nz = 0;
        switch (face) {
            case "front":  nz = 1; break;
            case "back":   nz = -1; break;
            case "left":   nx = -1; break;
            case "right":  nx = 1; break;
            case "top":    ny = 1; break;
            case "bottom": ny = -1; break;
        }

        // Vector from block center to player
        float vx = px - (bx + 0.5f);
        float vy = py - (by + 0.5f);
        float vz = pz - (bz + 0.5f);

        // Simple check: is the face normal pointing roughly toward player look direction?
        // dot(faceNormal, lookDirection) > 0 (face toward camera)
        float dotLook = nx * lookX + ny * lookY + nz * lookZ;
        // dot(faceNormal, vectorToPlayer) > 0 (face toward player position)
        float dotPlayer = nx * vx + ny * vy + nz * vz;

        // Tweak these thresholds for best results
        return dotLook > 0.3f || dotPlayer > 0;
    }

    // Updated to use the texture manager
    private void renderBlockFace(int x, int y, int z, Material type, String face) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        BlockRegistry.BlockInfo info = blockRegistry.getInfo(type);

        String textureName;
        switch (face) {
            case "top":    textureName = info.textureTop;    break;
            case "bottom": textureName = info.textureBottom; break;
            default:       textureName = info.textureSide;   break;
        }
        if (type == Material.AIR || textureName == null) {
            GL11.glPopMatrix();
            return;
        }

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
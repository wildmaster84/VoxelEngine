package engine.client;

import engine.client.common.Player;
import engine.common.block.BlockRegistry;
import engine.common.block.Material;
import engine.common.world.Chunk;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VoxelRenderer {
    private WorldView world;
    private BlockRegistry blockRegistry;
    private TextureManager textureManager;
    private static final int VIEW_DISTANCE = 4;

    private final Map<Chunk, Map<String, ChunkMesh>> chunkMeshesByTexture = new HashMap<>();
    private ShaderProgram shader;

    public static class AnimationInfo {
        public final int frameWidth, frameHeight, totalFrames, texWidth, texHeight;
        public AnimationInfo(int frameWidth, int frameHeight, int totalFrames, int texWidth, int texHeight) {
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.totalFrames = totalFrames;
            this.texWidth = texWidth;
            this.texHeight = texHeight;
        }
    }

    private final Map<String, AnimationInfo> animatedTextures = new HashMap<>();
    private final Map<String, Integer> animationFrames = new HashMap<>();
    private final Map<Chunk, Map<String, Integer>> chunkAnimatedFrame = new HashMap<>();

    public VoxelRenderer(WorldView world, BlockRegistry blockRegistry, TextureManager textureManager) {
        this.world = world;
        this.blockRegistry = blockRegistry;
        this.textureManager = textureManager;
    }

    public void detectAnimatedTextures() {
        for (String textureName : textureManager.getAllTextureNames()) {
            int texWidth = textureManager.getTextureWidth(textureName);
            int texHeight = textureManager.getTextureHeight(textureName);

            if (texHeight > texWidth && texHeight % texWidth == 0) {
                int frameWidth = texWidth;
                int frameHeight = texWidth;
                int totalFrames = texHeight / texWidth;
                animatedTextures.put(textureName, new AnimationInfo(frameWidth, frameHeight, totalFrames, texWidth, texHeight));
                animationFrames.put(textureName, 0);
            }
        }
    }

    public void initGL() {
        try {
            shader = new ShaderProgram("shaders/voxel.vert", "shaders/voxel.frag");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void rebuildChunkMeshes(Chunk chunk) {
        Map<String, ChunkMesh> old = chunkMeshesByTexture.get(chunk);
        if (old != null) for (ChunkMesh mesh : old.values()) mesh.free();

        Map<String, FloatBuffer> buffers = new HashMap<>();
        int baseX = chunk.getX() * Chunk.SIZE;
        int baseY = chunk.getY() * Chunk.SIZE;
        int baseZ = chunk.getZ() * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    Material type = chunk.getBlock(x, y, z).getType();
                    if (type == Material.AIR) continue;
                    BlockRegistry.BlockInfo info = blockRegistry.getInfo(type);

                    for (String face : new String[]{"front", "back", "left", "right", "top", "bottom"}) {
                        String textureName;
                        switch (face) {
                            case "top": textureName = info.textureTop; break;
                            case "bottom": textureName = info.textureBottom; break;
                            default: textureName = info.textureSide; break;
                        }
                        if (textureName == null) continue;
                        if (!shouldRenderFace(chunk, x, y, z, faceToDx(face), faceToDy(face), faceToDz(face), type))
                            continue;
                        FloatBuffer buf = buffers.computeIfAbsent(textureName, k -> BufferUtils.createFloatBuffer(1024 * 1024));

                        if (animatedTextures.containsKey(textureName)) {
                            AnimationInfo anim = animatedTextures.get(textureName);
                            int frameIdx = animationFrames.getOrDefault(textureName, 0);

                            float uMin = 0f;
                            float uMax = (float)anim.frameWidth / anim.texWidth;
                            float vMin = (float)(frameIdx * anim.frameHeight) / anim.texHeight;
                            float vMax = (float)((frameIdx + 1) * anim.frameHeight) / anim.texHeight;

                            putFace(buf, baseX + x, baseY + y, baseZ + z, type, face, uMin, uMax, vMin, vMax);
                        } else {
                            putFace(buf, baseX + x, baseY + y, baseZ + z, type, face, 0f, 1f, 0f, 1f);
                        }
                    }
                }
            }
        }

        Map<String, ChunkMesh> meshes = new HashMap<>();
        for (Map.Entry<String, FloatBuffer> entry : buffers.entrySet()) {
            FloatBuffer buf = entry.getValue();
            buf.flip();
            ChunkMesh mesh = new ChunkMesh();
            mesh.vertexCount = buf.limit() / 5;
            mesh.vboId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mesh.vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            meshes.put(entry.getKey(), mesh);
        }
        chunkMeshesByTexture.put(chunk, meshes);
    }

    public void removeChunkMeshes(Chunk chunk) {
        Map<String, ChunkMesh> old = chunkMeshesByTexture.get(chunk);
        if (old != null) for (ChunkMesh mesh : old.values()) mesh.free();
        chunkMeshesByTexture.remove(chunk);
        chunkAnimatedFrame.remove(chunk);
    }

    public void renderWorld(Player player) {
        for (Map.Entry<String, AnimationInfo> entry : animatedTextures.entrySet()) {
            AnimationInfo animInfo = entry.getValue();
            int frame = (int)((System.currentTimeMillis() / 100) % animInfo.totalFrames);
            animationFrames.put(entry.getKey(), frame);
        }

        int playerChunkX = (int)Math.floor(player.getX() / Chunk.SIZE);
        int playerChunkY = (int)Math.floor(player.getY() / Chunk.SIZE);
        int playerChunkZ = (int)Math.floor(player.getZ() / Chunk.SIZE);

        Collection<Chunk> chunksInView = world.getChunks();

        // 1. OPAQUE PASS
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);

        for (Chunk chunk : chunksInView) {
            int dx = chunk.getX() - playerChunkX;
            int dy = chunk.getY() - playerChunkY;
            int dz = chunk.getZ() - playerChunkZ;
            if (Math.abs(dx) > VIEW_DISTANCE ||
                Math.abs(dy) > VIEW_DISTANCE ||
                Math.abs(dz) > VIEW_DISTANCE) {
                continue;
            }

            boolean needsRebuild = false;
            Map<String, Integer> lastFrames = chunkAnimatedFrame.computeIfAbsent(chunk, k -> new HashMap<>());
            for (String animTex : animatedTextures.keySet()) {
                int currentFrame = animationFrames.get(animTex);
                int lastFrame = lastFrames.getOrDefault(animTex, -1);
                if (currentFrame != lastFrame) {
                    needsRebuild = true;
                    lastFrames.put(animTex, currentFrame);
                }
            }

            Map<String, ChunkMesh> texMeshes = chunkMeshesByTexture.get(chunk);
            if (needsRebuild || texMeshes == null) {
                rebuildChunkMeshes(chunk);
                texMeshes = chunkMeshesByTexture.get(chunk);
            }
            if (texMeshes != null) {
                for (Map.Entry<String, ChunkMesh> entry : texMeshes.entrySet()) {
                    String textureName = entry.getKey();
                    if (textureManager.isTextureOpaque(textureName)) {
                        textureManager.bindTexture(textureName);
                        renderChunkMesh(entry.getValue());
                    }
                }
            }
        }

        // 2. TRANSPARENT PASS
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);

        for (Chunk chunk : chunksInView) {
            int dx = chunk.getX() - playerChunkX;
            int dy = chunk.getY() - playerChunkY;
            int dz = chunk.getZ() - playerChunkZ;
            if (Math.abs(dx) > VIEW_DISTANCE ||
                Math.abs(dy) > VIEW_DISTANCE ||
                Math.abs(dz) > VIEW_DISTANCE) {
                continue;
            }
            Map<String, ChunkMesh> texMeshes = chunkMeshesByTexture.get(chunk);
            if (texMeshes != null) {
                for (Map.Entry<String, ChunkMesh> entry : texMeshes.entrySet()) {
                    String textureName = entry.getKey();
                    if (!textureManager.isTextureOpaque(textureName)) {
                        textureManager.bindTexture(textureName);
                        renderChunkMesh(entry.getValue());
                    }
                }
            }
        }
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private int faceToDx(String face) {
        switch (face) { case "left": return -1; case "right": return 1; default: return 0; }
    }
    private int faceToDy(String face) {
        switch (face) { case "top": return 1; case "bottom": return -1; default: return 0; }
    }
    private int faceToDz(String face) {
        switch (face) { case "front": return 1; case "back": return -1; default: return 0; }
    }

    private void putFace(FloatBuffer buf, int x, int y, int z, Material type, String face,
                        float uMin, float uMax, float vMin, float vMax) {
        float[][] positions = getFaceVertices(x, y, z, face);
        float[][] uvs = getFaceUVs(uMin, uMax, vMin, vMax);

        for (int i = 0; i < 6; i++) {
            buf.put(positions[i][0]);
            buf.put(positions[i][1]);
            buf.put(positions[i][2]);
            buf.put(uvs[i][0]);
            buf.put(uvs[i][1]);
        }
    }

    private float[][] getFaceVertices(int x, int y, int z, String face) {
        switch (face) {
            case "front":
                return new float[][]{
                    {x, y, z+1}, {x+1, y, z+1}, {x+1, y+1, z+1},
                    {x, y, z+1}, {x+1, y+1, z+1}, {x, y+1, z+1}
                };
            case "back":
                return new float[][]{
                    {x, y, z}, {x+1, y, z}, {x+1, y+1, z},
                    {x, y, z}, {x+1, y+1, z}, {x, y+1, z}
                };
            case "left":
                return new float[][]{
                    {x, y, z}, {x, y, z+1}, {x, y+1, z+1},
                    {x, y, z}, {x, y+1, z+1}, {x, y+1, z}
                };
            case "right":
                return new float[][]{
                    {x+1, y, z}, {x+1, y, z+1}, {x+1, y+1, z+1},
                    {x+1, y, z}, {x+1, y+1, z+1}, {x+1, y+1, z}
                };
            case "top":
                return new float[][]{
                    {x, y+1, z}, {x+1, y+1, z}, {x+1, y+1, z+1},
                    {x, y+1, z}, {x+1, y+1, z+1}, {x, y+1, z+1}
                };
            case "bottom":
                return new float[][]{
                    {x, y, z}, {x+1, y, z}, {x+1, y, z+1},
                    {x, y, z}, {x+1, y, z+1}, {x, y, z+1}
                };
            default:
                throw new IllegalArgumentException("Unknown face: " + face);
        }
    }

    private float[][] getFaceUVs(float uMin, float uMax, float vMin, float vMax) {
        return new float[][]{
            {uMin, vMax}, {uMax, vMax}, {uMax, vMin},
            {uMin, vMax}, {uMax, vMin}, {uMin, vMin}
        };
    }

    private void renderChunkMesh(ChunkMesh mesh) {
        if (mesh.vertexCount == 0) return;

        shader.use();
        shader.setUniform("tex", 0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mesh.vboId);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * 4, 0);

        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * 4, 3 * 4);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, mesh.vertexCount);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        shader.stop();
    }

    // Only render faces adjacent to air for water, for others if neighbor is transparent
    private boolean shouldRenderFace(Chunk chunk, int x, int y, int z, int dx, int dy, int dz, Material type) {
        int nx = x + dx, ny = y + dy, nz = z + dz;
        if (nx >= 0 && nx < Chunk.SIZE &&
            ny >= 0 && ny < Chunk.SIZE &&
            nz >= 0 && nz < Chunk.SIZE) {
            // Neighbor is in this chunk
            Material neighborType = chunk.getBlock(nx, ny, nz).getType();

            if (type == Material.WATER) {
                return neighborType == Material.AIR;
            } else {
                return isTransparent(neighborType);
            }
        } else {
            // Neighbor is in another chunk
            Chunk neighborChunk = world.getChunk(
                chunk.getX() + (nx < 0 ? -1 : (nx >= Chunk.SIZE ? 1 : 0)),
                chunk.getY() + (ny < 0 ? -1 : (ny >= Chunk.SIZE ? 1 : 0)),
                chunk.getZ() + (nz < 0 ? -1 : (nz >= Chunk.SIZE ? 1 : 0))
            );
            if (neighborChunk == null) {
                // If neighbor chunk isn't loaded, consider it air so face is visible
                return true;
            }
            int bx = (nx + Chunk.SIZE) % Chunk.SIZE;
            int by = (ny + Chunk.SIZE) % Chunk.SIZE;
            int bz = (nz + Chunk.SIZE) % Chunk.SIZE;
            Material neighborType = neighborChunk.getBlock(bx, by, bz).getType();

            if (type == Material.WATER) {
                return neighborType == Material.AIR;
            } else {
                return isTransparent(neighborType);
            }
        }
    }

    private boolean isTransparent(Material material) {
        return material == Material.AIR || material == Material.WATER;
    }
}
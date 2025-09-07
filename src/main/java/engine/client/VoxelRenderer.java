package engine.client;

import engine.client.common.Player;
import engine.common.block.BlockRegistry;
import engine.common.block.Material;
import engine.common.world.Chunk;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.*;

public class VoxelRenderer {
    private static final int VIEW_DISTANCE = 4;

    private final WorldView world;
    private final BlockRegistry blockRegistry;
    private final TextureManager textureManager;

    private final Map<Chunk, Map<String, ChunkMesh>> chunkMeshesByTexture = new HashMap<>();
    private final Map<String, AnimationInfo> animatedTextures = new HashMap<>();
    private final Map<String, Integer> animationFrames = new HashMap<>();
    private final Map<Chunk, Map<String, Integer>> chunkAnimatedFrame = new HashMap<>();

    private ShaderProgram shader;

    // === Animation Info ===
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

    // === Faces enum ===
    private enum Face {
        FRONT(0, 0, 1),
        BACK(0, 0, -1),
        LEFT(-1, 0, 0),
        RIGHT(1, 0, 0),
        TOP(0, 1, 0),
        BOTTOM(0, -1, 0);

        final int dx, dy, dz;
        Face(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    public VoxelRenderer(WorldView world, BlockRegistry blockRegistry, TextureManager textureManager) {
        this.world = world;
        this.blockRegistry = blockRegistry;
        this.textureManager = textureManager;
    }

    // === Setup ===
    public void detectAnimatedTextures() {
        for (String textureName : textureManager.getAllTextureNames()) {
            int texWidth = textureManager.getTextureWidth(textureName);
            int texHeight = textureManager.getTextureHeight(textureName);

            if (texHeight > texWidth && texHeight % texWidth == 0) {
                int totalFrames = texHeight / texWidth;
                animatedTextures.put(textureName,
                        new AnimationInfo(texWidth, texWidth, totalFrames, texWidth, texHeight));
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

    // === Chunk Mesh Building ===
    public void rebuildChunkMeshes(Chunk chunk) {
        // Free old meshes
        Map<String, ChunkMesh> old = chunkMeshesByTexture.remove(chunk);
        if (old != null) old.values().forEach(ChunkMesh::free);

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
                    for (Face face : Face.values()) {
                        String textureName = switch (face) {
                            case TOP -> info.textureTop;
                            case BOTTOM -> info.textureBottom;
                            default -> info.textureSide;
                        };
                        if (textureName == null) continue;
                        if (!shouldRenderFace(chunk, x, y, z, face, type)) continue;

                        FloatBuffer buf = buffers.computeIfAbsent(
                                textureName, k -> BufferUtils.createFloatBuffer(1024 * 1024));

                        if (animatedTextures.containsKey(textureName)) {
                            AnimationInfo anim = animatedTextures.get(textureName);
                            int frameIdx = animationFrames.getOrDefault(textureName, 0);

                            float uMax = (float) anim.frameWidth / anim.texWidth;
                            float vMin = (float) (frameIdx * anim.frameHeight) / anim.texHeight;
                            float vMax = (float) ((frameIdx + 1) * anim.frameHeight) / anim.texHeight;

                            putFace(buf, baseX + x, baseY + y, baseZ + z, face, 0f, uMax, vMin, vMax);
                        } else {
                            putFace(buf, baseX + x, baseY + y, baseZ + z, face, 0f, 1f, 0f, 1f);
                        }
                    }
                }
            }
        }

        Map<String, ChunkMesh> meshes = new HashMap<>();
        for (var entry : buffers.entrySet()) {
            FloatBuffer buf = entry.getValue();
            buf.flip();
            ChunkMesh mesh = new ChunkMesh();
            mesh.vertexCount = buf.limit() / 5;
            mesh.vboId = GL20.glGenBuffers();
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, mesh.vboId);
            GL20.glBufferData(GL20.GL_ARRAY_BUFFER, buf, GL20.GL_STATIC_DRAW);
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
            meshes.put(entry.getKey(), mesh);
        }
        chunkMeshesByTexture.put(chunk, meshes);
    }

    public void removeChunkMeshes(Chunk chunk) {
        Map<String, ChunkMesh> old = chunkMeshesByTexture.remove(chunk);
        if (old != null) old.values().forEach(ChunkMesh::free);
        chunkAnimatedFrame.remove(chunk);
    }

    // === Rendering ===
    public void renderWorld(Player player) {
        // Update animation frames
        for (var entry : animatedTextures.entrySet()) {
            AnimationInfo animInfo = entry.getValue();
            int frame = (int) ((System.currentTimeMillis() / 100) % animInfo.totalFrames);
            animationFrames.put(entry.getKey(), frame);
        }

        int playerChunkX = (int) Math.floor(player.getX() / Chunk.SIZE);
        int playerChunkY = (int) Math.floor(player.getY() / Chunk.SIZE);
        int playerChunkZ = (int) Math.floor(player.getZ() / Chunk.SIZE);

        Collection<Chunk> chunksInView = world.getChunks();

        // Render opaque then transparent
        renderPass(chunksInView, playerChunkX, playerChunkY, playerChunkZ, true);
        renderPass(chunksInView, playerChunkX, playerChunkY, playerChunkZ, false);
    }

    private void renderPass(Collection<Chunk> chunksInView, int px, int py, int pz, boolean opaque) {
        if (opaque) {
        	GL20.glDisable(GL20.GL_BLEND);
        	GL20.glDepthMask(true);
        } else {
        	GL20.glEnable(GL20.GL_BLEND);
        	GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        	GL20.glDepthMask(false);
        }

        for (Chunk chunk : chunksInView) {
            if (Math.abs(chunk.getX() - px) > VIEW_DISTANCE ||
                Math.abs(chunk.getY() - py) > VIEW_DISTANCE ||
                Math.abs(chunk.getZ() - pz) > VIEW_DISTANCE) {
                continue;
            }

            boolean needsRebuild = false;
            Map<String, Integer> lastFrames = chunkAnimatedFrame.computeIfAbsent(chunk, k -> new HashMap<>());
            for (String animTex : animatedTextures.keySet()) {
                int currentFrame = animationFrames.get(animTex);
                if (currentFrame != lastFrames.getOrDefault(animTex, -1)) {
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
                for (var entry : texMeshes.entrySet()) {
                    String textureName = entry.getKey();
                    if (textureManager.isTextureOpaque(textureName) == opaque) {
                        textureManager.bindTexture(textureName);
                        renderChunkMesh(entry.getValue());
                    }
                }
            }
        }

        if (!opaque) {
        	GL20.glDepthMask(true);
        	GL20.glDisable(GL20.GL_BLEND);
        }
    }

    // === Face Helpers ===
    private void putFace(FloatBuffer buf, int x, int y, int z, Face face,
                         float uMin, float uMax, float vMin, float vMax) {
        float[][] positions = getFaceVertices(x, y, z, face);
        float[][] uvs = getFaceUVs(uMin, uMax, vMin, vMax);
        for (int i = 0; i < 6; i++) {
            buf.put(positions[i][0]).put(positions[i][1]).put(positions[i][2]);
            buf.put(uvs[i][0]).put(uvs[i][1]);
        }
    }

    private float[][] getFaceVertices(int x, int y, int z, Face face) {
        return switch (face) {
            case FRONT -> new float[][]{{x, y, z+1},{x+1,y,z+1},{x+1,y+1,z+1},{x,y,z+1},{x+1,y+1,z+1},{x,y+1,z+1}};
            case BACK -> new float[][]{{x, y, z},{x+1,y,z},{x+1,y+1,z},{x,y,z},{x+1,y+1,z},{x,y+1,z}};
            case LEFT -> new float[][]{{x,y,z},{x,y,z+1},{x,y+1,z+1},{x,y,z},{x,y+1,z+1},{x,y+1,z}};
            case RIGHT -> new float[][]{{x+1,y,z},{x+1,y,z+1},{x+1,y+1,z+1},{x+1,y,z},{x+1,y+1,z+1},{x+1,y+1,z}};
            case TOP -> new float[][]{{x,y+1,z},{x+1,y+1,z},{x+1,y+1,z+1},{x,y+1,z},{x+1,y+1,z+1},{x,y+1,z+1}};
            case BOTTOM -> new float[][]{{x,y,z},{x+1,y,z},{x+1,y,z+1},{x,y,z},{x+1,y,z+1},{x,y,z+1}};
        };
    }

    private float[][] getFaceUVs(float uMin, float uMax, float vMin, float vMax) {
        return new float[][]{
            {uMin, vMax}, {uMax, vMax}, {uMax, vMin},
            {uMin, vMax}, {uMax, vMin}, {uMin, vMin}
        };
    }

    // === Rendering Helpers ===
    private void renderChunkMesh(ChunkMesh mesh) {
        if (mesh.vertexCount == 0) return;

        shader.use();
        shader.setUniform("tex", 0);

        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, mesh.vboId);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, 20, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, 20, 12);

        GL20.glDrawArrays(GL20.GL_TRIANGLES, 0, mesh.vertexCount);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        shader.stop();
    }

    // === Neighbor Check ===
    private boolean shouldRenderFace(Chunk chunk, int x, int y, int z, Face face, Material type) {
        int nx = x + face.dx, ny = y + face.dy, nz = z + face.dz;

        Material neighborType;
        if (nx >= 0 && nx < Chunk.SIZE && ny >= 0 && ny < Chunk.SIZE && nz >= 0 && nz < Chunk.SIZE) {
            neighborType = chunk.getBlock(nx, ny, nz).getType();
        } else {
            Chunk neighborChunk = world.getChunk(
                chunk.getX() + Integer.signum(nx / Chunk.SIZE),
                chunk.getY() + Integer.signum(ny / Chunk.SIZE),
                chunk.getZ() + Integer.signum(nz / Chunk.SIZE)
            );
            if (neighborChunk == null) return true; // Neighbor chunk not loaded → render
            int bx = (nx + Chunk.SIZE) % Chunk.SIZE;
            int by = (ny + Chunk.SIZE) % Chunk.SIZE;
            int bz = (nz + Chunk.SIZE) % Chunk.SIZE;
            neighborType = neighborChunk.getBlock(bx, by, bz).getType();
        }

        return (type == Material.WATER) ? neighborType == Material.AIR : isTransparent(neighborType);
    }

    private boolean isTransparent(Material material) {
        return material == Material.AIR || material == Material.WATER;
    }
}

package engine.client;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class ChunkMesh {
    public int vboId = -1;
    public int vertexCount = 0;

    public void free() {
        if (vboId != -1) {
            GL15.glDeleteBuffers(vboId);
            vboId = -1;
        }
    }
}
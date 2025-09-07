package engine.client;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TextureManager {
    private final Map<String, Integer> textureIds = new HashMap<>();
    private final Map<String, Integer> textureWidths = new HashMap<>();
    private final Map<String, Integer> textureHeights = new HashMap<>();
    // Automatically detect if a texture is opaque or not
    private final Map<String, Boolean> textureIsOpaque = new HashMap<>();

    // Load texture from file, store OpenGL id, dimensions, and opacity info
    public void loadTexture(String name, String path) {
        try {
            BufferedImage image = ImageIO.read(new File(path));
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

            boolean isOpaque = true; // Assume opaque until a transparent pixel is found

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    int alpha = (pixel >> 24) & 0xFF;
                    if (alpha < 255) {
                        isOpaque = false;
                    }
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                    buffer.put((byte) (pixel & 0xFF));         // B
                    buffer.put((byte) alpha);                  // A
                }
            }
            buffer.flip();

            int texId = GL20.glGenTextures();
            GL20.glBindTexture(GL20.GL_TEXTURE_2D, texId);

            GL20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, width, height, 0,
            		GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buffer);

            GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
            GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
            //GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
            //GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

            textureIds.put(name, texId);
            textureWidths.put(name, width);
            textureHeights.put(name, height);
            textureIsOpaque.put(name, isOpaque);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture: " + path, e);
        }
    }

    // Bind a texture by name
    public void bindTexture(String name) {
        Integer texId = textureIds.get(name);
        if (texId != null) {
        	GL20.glBindTexture(GL20.GL_TEXTURE_2D, texId);
        }
    }

    // Get texture width
    public int getTextureWidth(String name) {
        return textureWidths.getOrDefault(name, 1);
    }

    // Get texture height
    public int getTextureHeight(String name) {
        return textureHeights.getOrDefault(name, 1);
    }

    // Get all loaded texture names
    public Set<String> getAllTextureNames() {
        return textureIds.keySet();
    }

    // Query if a texture is opaque (no transparent pixels)
    public boolean isTextureOpaque(String name) {
        return textureIsOpaque.getOrDefault(name, true);
    }

    // Optional: delete all textures
    public void cleanup() {
        for (int texId : textureIds.values()) {
        	GL20.glDeleteTextures(texId);
        }
        textureIds.clear();
        textureWidths.clear();
        textureHeights.clear();
        textureIsOpaque.clear();
    }
}
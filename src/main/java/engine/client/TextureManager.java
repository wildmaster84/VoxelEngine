package engine.client;

import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class TextureManager {
    private final HashMap<String, Integer> textureMap = new HashMap<>();

    // Load a texture from disk and upload to OpenGL
    public void loadTexture(String name, String filePath) {
        try {
            BufferedImage image = ImageIO.read(new File(filePath));
            int width = image.getWidth();
            int height = image.getHeight();

            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            buffer.flip();

            int textureID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            textureMap.put(name, textureID);
        } catch (Exception e) {
            System.err.println("Failed to load texture: " + filePath);
            e.printStackTrace();
        }
    }

    // Bind a named texture for rendering
    public void bindTexture(String name) {
        Integer id = textureMap.get(name);
        if (id != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        } else {
            System.err.println("Texture not found: " + name);
        }
    }
}
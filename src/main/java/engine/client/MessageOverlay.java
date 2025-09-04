package engine.client;

import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MessageOverlay {
    private static class Message {
        final String text;
        final long expiryMillis;

        Message(String text, long durationMillis) {
            this.text = text;
            this.expiryMillis = System.currentTimeMillis() + durationMillis;
        }
    }

    private static List<Message> messages = new ArrayList<>();

    public static void addMessage(String text) {
        addMessage(text, 8000); // Show for 8 seconds
    }

    public static void addMessage(String text, long durationMillis) {
        messages.add(new Message(text, durationMillis));
    }

    // Font rendering variables
    private static ByteBuffer fontBuffer;
    private static STBTTFontinfo fontInfo;
    private static int fontSize = 24;

    static {
        try {
            byte[] fontBytes = Files.readAllBytes(Paths.get("assets/fonts/Roboto-Regular.ttf"));
            if (fontBytes.length == 0)
                throw new IOException("Font file is empty!");
            fontBuffer = ByteBuffer.allocateDirect(fontBytes.length);
            fontBuffer.put(fontBytes);
            fontBuffer.flip();

            fontInfo = STBTTFontinfo.malloc();
            if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
                throw new RuntimeException("Failed to initialize font (bad TTF?)");
            }
        } catch (Exception e) {
            System.err.println("Font load error: " + e);
            fontInfo = null;
        }
    }

    public static void render() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 800, 600, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        long now = System.currentTimeMillis();
        int y = 40;

        Iterator<Message> iter = messages.iterator();
        while (iter.hasNext()) {
            Message msg = iter.next();
            if (msg.expiryMillis < now) {
                iter.remove();
            }
        }

        if (fontInfo != null) {
            for (Message msg : messages) {
                drawText(20, y, msg.text);
                y += fontSize + 8;
            }
        } else {
            // Optionally draw a fallback message if font not loaded
            GL11.glColor3f(1f, 0f, 0f);
            GL11.glRasterPos2i(20, 40);
            // No text renderer, so just do nothing or draw a colored quad as fallback
        }

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    // Very basic text rendering using STB Truetype and OpenGL immediate mode.
    private static void drawText(int xPos, int yPos, String text) {
        if (fontInfo == null) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pAdvanceWidth = stack.mallocInt(1);
            IntBuffer pLeftSideBearing = stack.mallocInt(1);
            float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, fontSize);

            int x = xPos;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                int cp = c; // ASCII only for simplicity
                STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, cp, pAdvanceWidth, pLeftSideBearing);

                IntBuffer pX0 = stack.mallocInt(1);
                IntBuffer pY0 = stack.mallocInt(1);
                IntBuffer pX1 = stack.mallocInt(1);
                IntBuffer pY1 = stack.mallocInt(1);

                STBTruetype.stbtt_GetCodepointBitmapBox(fontInfo, cp, scale, scale, pX0, pY0, pX1, pY1);

                int w = pX1.get(0) - pX0.get(0);
                int h = pY1.get(0) - pY0.get(0);

                ByteBuffer bitmap = STBTruetype.stbtt_GetCodepointBitmap(fontInfo, 0, scale, cp, pX1, pY1, pX0, pY0);

                if (bitmap != null) {
                    GL11.glRasterPos2i(x + pX0.get(0), yPos - pY0.get(0));
                    for (int row = 0; row < h; row++) {
                        for (int col = 0; col < w; col++) {
                            int idx = row * w + col;
                            byte value = bitmap.get(idx);
                            if (value != 0) {
                                GL11.glColor3f(1f, 1f, 1f);
                                GL11.glBegin(GL11.GL_POINTS);
                                GL11.glVertex2i(x + col, yPos + row);
                                GL11.glEnd();
                            }
                        }
                    }
                    STBTruetype.stbtt_FreeBitmap(bitmap);
                }
                x += (int)(pAdvanceWidth.get(0) * scale);
            }
        }
    }
}
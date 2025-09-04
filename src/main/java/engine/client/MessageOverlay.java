package engine.client;

import org.lwjgl.opengl.GL11;

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
    	System.out.println("Added message " + text);
        addMessage(text, 8000); // Default: show for 3 seconds
    }

    public static void addMessage(String text, long durationMillis) {
    	System.out.println("Added message to map " + text);
        messages.add(new Message(text, durationMillis));
    }

    public static void render() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 800, 600, 0, -1, 1); // Window size, adjust as needed
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        long now = System.currentTimeMillis();
        int y = 40; // Start a bit down from the top

        // Remove expired messages
        Iterator<Message> iter = messages.iterator();
        while (iter.hasNext()) {
            Message msg = iter.next();
            if (msg.expiryMillis < now) {
            	System.out.println("Removed message from map ");
                iter.remove();
            }
        }

        for (Message msg : messages) {
        	System.out.println("drawing message from map ");
            drawText(20, y, msg.text);
            y += 24; // Spacing between messages
        }

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    // Dummy text renderer: replace with your bitmap/font rendering
    private static void drawText(int x, int y, String text) {
        // For a real implementation: use a bitmap font renderer, or stb_truetype.
        // For now, just draw a colored quad as placeholder.
        GL11.glColor3f(0f, 0f, 0f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x - 4, y - 4);
        GL11.glVertex2f(x + 200, y - 4);
        GL11.glVertex2f(x + 200, y + 20);
        GL11.glVertex2f(x - 4, y + 20);
        GL11.glEnd();
        GL11.glColor3f(1f, 1f, 1f);
        // You need to use a bitmap font (e.g., via STB TrueType or similar) to render actual text here!
        // For demonstration only.
    }
}
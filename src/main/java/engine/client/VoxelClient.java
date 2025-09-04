package engine.client;

import com.esotericsoftware.kryonet.*;

import engine.common.block.Block;
import engine.common.block.BlockRegistry;
import engine.common.network.NetworkManager;
import engine.common.network.packet.BlockUpdatePacket;
import engine.common.network.packet.ChunkDataPacket;
import engine.common.network.packet.PlayerChatPacket;
import engine.common.network.packet.PlayerJoinPacket;
import engine.common.network.packet.PlayerMovePacket;
import engine.common.world.Chunk;
import engine.client.common.Player;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.util.*;

public class VoxelClient {
    private NetworkManager network = new NetworkManager();
    private WorldView worldView = new WorldView(); // In-memory only
    private BlockRegistry blockRegistry = BlockRegistry.createDefault();
    private Player localPlayer = new Player(UUID.randomUUID(), "Player");
    private Map<String, Player> otherPlayers = new HashMap<>();
    private TextureManager textureManager = new TextureManager();
    private VoxelRenderer renderer = new VoxelRenderer(worldView, blockRegistry, textureManager);
    private MessageOverlay overlay = new MessageOverlay();

    @FunctionalInterface
    interface PacketHandler<T> {
        void handle(Connection connection, T packet);
    }
    private final Map<Class<?>, PacketHandler<?>> packetHandlers = new HashMap<>();

    // Camera state
    private float camYaw = 0, camPitch = 0;
    private double lastMouseX = 400, lastMouseY = 300;

    public static void main(String[] args) throws Exception {
        new VoxelClient().start();
    }

    public void start() throws Exception {
        registerPacketHandlers();

        network.startClient("localhost", 54555, 54777);
        Client client = network.getClient();

        client.addListener(new Listener() {
            public void received(Connection connection, Object packet) {
                PacketHandler handler = packetHandlers.get(packet.getClass());
                if (handler != null) {
                    handler.handle(connection, packet);
                }
            }
        });

        // Send join packet (client sends, but does NOT handle PlayerJoinPacket)
        PlayerJoinPacket join = new PlayerJoinPacket();
        join.playerId = localPlayer.getUniqueID().toString();
        join.x = localPlayer.getX(); join.y = localPlayer.getY(); join.z = localPlayer.getZ();
        client.sendTCP(join);

        // LWJGL window setup
        tick(client);
    }

    private void tick(Client client) {
        if (!GLFW.glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
        long window = GLFW.glfwCreateWindow(800, 600, "Voxel Client", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        // Hide and capture mouse
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(window, lastMouseX, lastMouseY);

        GL11.glClearColor(0.5f, 0.7f, 1f, 1f); // sky blue

        // --- Jumping variables ---
        float velocityY = 0f;
        Block blockBelow = worldView.getBlock((int)localPlayer.getX(), (int)(localPlayer.getY() - 0.01f), (int)localPlayer.getZ());
        boolean onGround = blockBelow != null && blockBelow.isSolid();
        boolean jumping = false;
        float gravity = -0.06f; // Adjust for world scale/speed
        float jumpStrength = 0.3f; // Adjust for desired jump height
        
        int frames = 0;
        long lastTime = System.nanoTime();
        float fps = 0.0f;
        
        loadAllTexturesFromRegistry();

        while (!GLFW.glfwWindowShouldClose(window)) {
        	
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            // --- Mouse look (first person camera) ---
            double[] mx = new double[1], my = new double[1];
            GLFW.glfwGetCursorPos(window, mx, my);
            double mouseX = mx[0], mouseY = my[0];
            float sensitivity = 0.15f;
            float dx = (float)(mouseX - lastMouseX);
            float dy = (float)(mouseY - lastMouseY);
            camYaw -= dx * sensitivity;    // +dx turns right
            camPitch -= dy * sensitivity;  // -dy looks up
            camPitch = Math.max(-89f, Math.min(89f, camPitch)); // Clamp pitch
            lastMouseX = mouseX;
            lastMouseY = mouseY;

            // --- WASD movement (XZ plane, ignores pitch) ---
            float moveSpeed = 3.3f; // Adjust for reasonable walking speed
            float yawRad = (float)Math.toRadians(camYaw);

            // Forward vector (XZ plane)
            float forwardX = (float)Math.sin(yawRad);
            float forwardZ = (float)Math.cos(yawRad);

            // Right vector (perpendicular in XZ plane)
            float rightX = (float)Math.sin(yawRad + Math.PI / 2.0);
            float rightZ = (float)Math.cos(yawRad + Math.PI / 2.0);

            float px = localPlayer.getX(), py = localPlayer.getY(), pz = localPlayer.getZ();

            float dxMove = 0, dzMove = 0;
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
                dxMove += forwardX * moveSpeed;
                dzMove += forwardZ * moveSpeed;
            }
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
                dxMove -= forwardX * moveSpeed;
                dzMove -= forwardZ * moveSpeed;
            }
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
                dxMove -= rightX * moveSpeed;
                dzMove -= rightZ * moveSpeed;
            }
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
                dxMove += rightX * moveSpeed;
                dzMove += rightZ * moveSpeed;
            }

            // --- Jumping logic ---
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS && onGround) {
            	if (!jumping) {
            		velocityY = jumpStrength;
            		jumping = true;
            	}
            }
            
         // Gravity
            velocityY += gravity;
            py += velocityY;

            Block blockBelowAfter = worldView.getBlock((int)px, (int)(py - 0.01f), (int)pz);
            if (velocityY < 0 && blockBelowAfter != null && blockBelowAfter.isSolid()) {
                py = (float)Math.ceil(py);
                velocityY = 0f;
                jumping = false;
            }

            px += dxMove;
            pz += dzMove;
            localPlayer.setPosition(px, py, pz);

            // Send movement packet
            PlayerMovePacket pm = new PlayerMovePacket();
            pm.playerId = localPlayer.getUniqueID().toString();
            pm.x = localPlayer.getX(); pm.y = localPlayer.getY(); pm.z = localPlayer.getZ();
            pm.yaw = camYaw; pm.pitch = camPitch;
            client.sendTCP(pm);

            // --- First-person camera setup ---
            float camHeight = 1.7f; // Typical eye height in blocks
            float camX = px;
            float camY = py + camHeight;
            float camZ = pz;

            float pitchRad = (float)Math.toRadians(camPitch);
            float lookDirX = (float)(Math.cos(pitchRad) * Math.sin(yawRad));
            float lookDirY = (float)(Math.sin(pitchRad));
            float lookDirZ = (float)(Math.cos(pitchRad) * Math.cos(yawRad));

            float targetX = camX + lookDirX;
            float targetY = camY + lookDirY;
            float targetZ = camZ + lookDirZ;

            float aspect = 800f / 600f;
            Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(70), aspect, 0.1f, 1000f);
            Matrix4f view = new Matrix4f().lookAt(
                camX, camY, camZ,
                targetX, targetY, targetZ,
                0, 1, 0
            );
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer projBuffer = stack.mallocFloat(16);
                FloatBuffer viewBuffer = stack.mallocFloat(16);
                projection.get(projBuffer);
                view.get(viewBuffer);

                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glLoadMatrixf(projBuffer);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glLoadMatrixf(viewBuffer);
            }


            renderer.renderWorld(localPlayer);
            MessageOverlay.render();
            
         // Calculate FPS
            
            
            frames++;
            long now = System.nanoTime();
            // Update FPS every second
            if (now - lastTime >= 1_000_000_000L) {
                fps = frames * 1_000_000_000.0f / (now - lastTime);
                frames = 0;
                lastTime = now;
            }

            // Other debug info
            String playerPos = "X:" + localPlayer.getX() + " Y:" + localPlayer.getY() + "Z:" + localPlayer.getX(); // e.g. "X:123 Y:64 Z:42"
            long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            String memStr = String.format("Mem: %.2f MB", usedMem / 1024.0 / 1024.0);

            // Combine messages
            String debugString = String.format("FPS: %.2f  |  %s  |  %s | Chunks: %s", fps, playerPos, memStr, worldView.getChunks().size());

            // Display in overlay
            MessageOverlay.addMessage(debugString, 200); // Show for 1 second (refreshes each frame)

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    private void registerPacketHandlers() {
        // Only handle packets that make sense for the client to process

        // Handle chunk data sent by server
        packetHandlers.put(ChunkDataPacket.class, (PacketHandler<ChunkDataPacket>) (connection, cd) -> {
            Chunk chunk = Chunk.fromNetwork(cd.chunkX, cd.chunkY, cd.chunkZ, cd.blockTypes);
            worldView.setChunk(chunk);
            //System.out.println("Received chunk: " + cd.chunkX + "," + cd.chunkY + "," + cd.chunkZ);
        });

        // Handle block update packet
        packetHandlers.put(BlockUpdatePacket.class, (PacketHandler<BlockUpdatePacket>) (connection, bu) -> {
            Chunk chunk = worldView.getChunk(bu.chunkX, bu.chunkY, bu.chunkZ);
            if (chunk != null) {
                chunk.setBlock(bu.x, bu.y, bu.z, new Block(bu.blockType));
            }
        });

        packetHandlers.put(PlayerChatPacket.class, (PacketHandler<PlayerChatPacket>) (connection, pc) -> {
        	MessageOverlay.addMessage(pc.message);
        });
        // Handle other player movement packet
        packetHandlers.put(PlayerMovePacket.class, (PacketHandler<PlayerMovePacket>) (connection, pm) -> {
            // Don't update localPlayer from network
            if (!localPlayer.getUniqueID().toString().equals(pm.playerId)) {
            	Player p = otherPlayers.get(pm.playerId);
                if (p == null) {
                    p = new Player(UUID.randomUUID(), "Player");
                    otherPlayers.put(pm.playerId, p);
                }
                p.setPosition(pm.x, pm.y, pm.z);
                p.setYaw(pm.yaw);
                p.setPitch(pm.pitch);
            }
        });

        // Do NOT handle PlayerJoinPacket on client (only server)
    }

    public void loadAllTexturesFromRegistry() {
        for (BlockRegistry.BlockInfo info : blockRegistry.getAllBlockInfos()) {
            if (info.textureTop != null)
                textureManager.loadTexture(info.textureTop, "textures/" + info.textureTop);
            if (info.textureSide != null)
                textureManager.loadTexture(info.textureSide, "textures/" + info.textureSide);
            if (info.textureBottom != null)
                textureManager.loadTexture(info.textureBottom, "textures/" + info.textureBottom);
        }
    }
}
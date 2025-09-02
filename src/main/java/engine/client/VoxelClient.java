package engine.client;

import com.esotericsoftware.kryonet.*;

import engine.common.block.Block;
import engine.common.block.BlockRegistry;
import engine.common.network.NetworkManager;
import engine.common.network.packet.BlockUpdatePacket;
import engine.common.network.packet.ChunkDataPacket;
import engine.common.network.packet.PlayerJoinPacket;
import engine.common.network.packet.PlayerMovePacket;
import engine.common.player.Player;
import engine.common.world.Chunk;

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
    private Player localPlayer = new Player();
    private Map<String, Player> otherPlayers = new HashMap<>();
    private TextureManager textureManager = new TextureManager();
    private VoxelRenderer renderer = new VoxelRenderer(worldView, blockRegistry, textureManager);

    @FunctionalInterface
    interface PacketHandler<T> {
        void handle(Connection connection, T packet);
    }
    private final Map<Class<?>, PacketHandler<?>> packetHandlers = new HashMap<>();

    // Camera state
    private float camYaw = 0, camPitch = 45, camDist = 1;
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
        join.playerId = "LocalPlayer";
        join.x = localPlayer.getX(); join.y = localPlayer.getY(); join.z = localPlayer.getZ();
        client.sendTCP(join);

        // LWJGL window setup
        if (!GLFW.glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
        long window = GLFW.glfwCreateWindow(800, 600, "Voxel Client", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        // Hide and capture mouse
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(window, lastMouseX, lastMouseY);

        GL11.glClearColor(0.5f, 0.7f, 1f, 1f); // sky blue

        while (!GLFW.glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            
            // --- Mouse look (orbit camera) ---
            double[] mx = new double[1], my = new double[1];
            GLFW.glfwGetCursorPos(window, mx, my);
            double mouseX = mx[0], mouseY = my[0];
            float sensitivity = 0.15f;
            float dx = (float)(mouseX - lastMouseX);
            float dy = (float)(mouseY - lastMouseY);
            camYaw -= dx * sensitivity;
            camPitch -= dy * sensitivity;
            camPitch = Math.max(-89f, Math.min(89f, camPitch)); // Clamp pitch
            lastMouseX = mouseX;
            lastMouseY = mouseY;

            // --- WASD movement relative to camera yaw ---
            float moveSpeed = 1.1f;
            float yawRad = (float)Math.toRadians(camYaw);

            // Forward vector (XZ plane)
            float forwardX = (float)Math.sin(yawRad);
            float forwardZ = (float)Math.cos(yawRad);

            // Right vector (XZ plane), perpendicular to forward
            float rightX = (float)Math.sin(yawRad + Math.PI / 2.0);
            float rightZ = (float)Math.cos(yawRad + Math.PI / 2.0);

            float px = localPlayer.getX(), py = localPlayer.getY(), pz = localPlayer.getZ();

            if(GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W)==GLFW.GLFW_PRESS) {
                px -= forwardX * moveSpeed;
                pz -= forwardZ * moveSpeed;
            }
            if(GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S)==GLFW.GLFW_PRESS) {
                px += forwardX * moveSpeed;
                pz += forwardZ * moveSpeed;
            }
            if(GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A)==GLFW.GLFW_PRESS) {
                px -= rightX * moveSpeed;
                pz -= rightZ * moveSpeed;
            }
            if(GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D)==GLFW.GLFW_PRESS) {
                px += rightX * moveSpeed;
                pz += rightZ * moveSpeed;
            }
            localPlayer.setPosition(px, py, pz);

            // Send movement packet
            PlayerMovePacket pm = new PlayerMovePacket();
            pm.playerId = "LocalPlayer";
            pm.x = localPlayer.getX(); pm.y = localPlayer.getY(); pm.z = localPlayer.getZ();
            pm.yaw = camYaw; pm.pitch = camPitch;
            client.sendTCP(pm);

            // --- Camera Setup (orbit style) ---
            float radius = camDist;
            float pitchRad = (float)Math.toRadians(camPitch);
            float camX = px + (float)(radius * Math.cos(pitchRad) * Math.sin(yawRad));
            float camY = py - (float)(radius * Math.sin(pitchRad));
            float camZ = pz + (float)(radius * Math.cos(pitchRad) * Math.cos(yawRad));

            float aspect = 800f / 600f;
            Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(70), aspect, 0.1f, 1000f);
            Matrix4f view = new Matrix4f().lookAt(
                camX, camY, camZ,
                px, py, pz,
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
            
            loadAllTexturesFromRegistry();

            renderer.renderWorld(localPlayer);

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
            System.out.println("Received chunk: " + cd.chunkX + "," + cd.chunkY + "," + cd.chunkZ);
        });

        // Handle block update packet
        packetHandlers.put(BlockUpdatePacket.class, (PacketHandler<BlockUpdatePacket>) (connection, bu) -> {
            Chunk chunk = worldView.getChunk(bu.chunkX, bu.chunkY, bu.chunkZ);
            if (chunk != null) {
                chunk.setBlock(bu.x, bu.y, bu.z, new Block(bu.blockType));
            }
        });

        // Handle other player movement packet
        packetHandlers.put(PlayerMovePacket.class, (PacketHandler<PlayerMovePacket>) (connection, pm) -> {
            // Don't update localPlayer from network
            if (!"LocalPlayer".equals(pm.playerId)) {
                Player p = otherPlayers.get(pm.playerId);
                if (p == null) {
                    p = new Player();
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
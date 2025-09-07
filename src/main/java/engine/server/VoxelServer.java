package engine.server;

import com.esotericsoftware.kryonet.*;

import engine.common.block.Block;
import engine.common.block.BlockRegistry;
import engine.common.network.NetworkManager;
import engine.common.network.packet.BlockUpdatePacket;
import engine.common.network.packet.ChunkDataPacket;
import engine.common.network.packet.PlayerChatPacket;
import engine.common.network.packet.PlayerJoinPacket;
import engine.common.network.packet.PlayerMovePacket;
import engine.common.player.Player;
import engine.common.world.Chunk;
import engine.common.world.DefaultChunkGenerator;
import server.event.EventManager;
import server.event.player.PlayerChatEvent;
import server.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class VoxelServer {
 private NetworkManager network = new NetworkManager();
 private World world = new World(new File("serverworld"));
 private BlockRegistry blockRegistry = BlockRegistry.createDefault();
 private ChunkGenerator chunkGenerator = new DefaultChunkGenerator(1, 128);
 private Map<String, Player> players = new ConcurrentHashMap<>();
 private server.Server serverInstance;

 // Track which chunks have been sent to which connection
 private Map<Connection, Set<String>> sentChunks = new ConcurrentHashMap<>();
 
 private static final int TICKS_PER_SECOND = 30;
 private static final long TICK_INTERVAL_MS = 1000 / TICKS_PER_SECOND;
 private volatile int tickCount = 0;
 private volatile double tps = TICKS_PER_SECOND;
 private volatile long lastTpsUpdate = System.currentTimeMillis();

 @FunctionalInterface
 interface PacketHandler<T> {
     void handle(Connection connection, T packet);
 }
 private final Map<Class<?>, PacketHandler<?>> packetHandlers = new HashMap<>();

 public static void main(String[] args) throws Exception {
     new VoxelServer().start();
 }

 private void start() throws Exception {
     registerPacketHandlers();

     network.startServer(54555, 54777);
     Server server = network.getServer();

     server.addListener(new Listener() {
         public void received(Connection connection, Object packet) {
             PacketHandler handler = packetHandlers.get(packet.getClass());
             if (handler != null) {
                 handler.handle(connection, packet);
             }
         }

         @Override
         public void disconnected(Connection connection) {
             // Cleanup sentChunks for this connection
             sentChunks.remove(connection);
         }
     });
     
     Thread tickThread = new Thread(() -> {
         long lastTickTime = System.currentTimeMillis();
         while (true) {
             long start = System.currentTimeMillis();
             tick();

             // TPS calculation every second
             if (start - lastTpsUpdate >= 1000) {
                 tps = tickCount * 1000.0 / (start - lastTpsUpdate);
                 tickCount = 0;
                 lastTpsUpdate = start;
             }

             long elapsed = System.currentTimeMillis() - start;
             long sleepTime = TICK_INTERVAL_MS - elapsed;
             if (sleepTime > 0) {
                 try {
                     Thread.sleep(sleepTime);
                 } catch (InterruptedException e) {
                     // Handle interrupt
                     break;
                 }
             }
         }
     }, "Server-Tick-Thread");
     tickThread.setDaemon(true);
     tickThread.start();
     
     //init Server instance
     serverInstance = new server.Server();
     RegisterServerEvents();
     serverInstance.start(this);
     
 }
 
 private void tick() {
     // Example: world/game logic, entity updates, etc.
     // For now, just increment tick count for TPS calculation
     tickCount++;     
 }

 private void sendChunkIfNotSent(Connection connection, int cx, int cy, int cz) {
     String chunkKey = cx + "," + cy + "," + cz;
     Set<String> sent = sentChunks.computeIfAbsent(connection, k -> ConcurrentHashMap.newKeySet());
     if (!sent.contains(chunkKey)) {
         Chunk chunk = world.getOrCreateChunk(cx, cy, cz, chunkGenerator, blockRegistry);
         ChunkDataPacket chunkPacket = new ChunkDataPacket(
             cx, cy, cz,
             chunk.serializeBlocks()
         );
         connection.sendTCP(chunkPacket);
         sent.add(chunkKey);
     } else {
         // Optional: System.out.println("Server: Skipped sending duplicate chunk " + chunkKey);
     }
 }
 
 private void RegisterServerEvents() {
 	EventManager eventManager = serverInstance.getEventManager();
 	
 	eventManager.registerListener(PlayerChatEvent.class, new server.event.EventListener<PlayerChatEvent>() {
		@Override
		public void handle(PlayerChatEvent event) {
			Player player = event.getPlayer();
 	        Connection conn = player.getConnection(); // Or from a map
 	        PlayerChatPacket chatPacket = new PlayerChatPacket();
 	        chatPacket.playerId = player.getUniqueID().toString();
 	        chatPacket.message = event.getText();
 	        conn.sendTCP(chatPacket);

		}
 	});
 }

 private void registerPacketHandlers() {

     packetHandlers.put(PlayerJoinPacket.class, (PacketHandler<PlayerJoinPacket>) (connection, pj) -> {
         Player player = new Player(UUID.randomUUID(), "Player", connection);
         player.setPosition(pj.x, pj.y, pj.z);
         players.put(pj.playerId, player);
         serverInstance.getEventManager().fireEvent(new PlayerJoinEvent(player));

         int playerChunkX = (int) Math.floor(player.getX() / Chunk.SIZE);
         int playerChunkZ = (int) Math.floor(player.getZ() / Chunk.SIZE);
         for (int cx = playerChunkX - 2; cx <= playerChunkX + 2; cx++) {
             for (int cz = playerChunkZ - 2; cz <= playerChunkZ + 2; cz++) {
                 sendChunkIfNotSent(connection, cx, 0, cz);
             }
         }
     });

     packetHandlers.put(PlayerMovePacket.class, (PacketHandler<PlayerMovePacket>) (connection, pm) -> {
         Player p = players.get(pm.playerId);
         if (p != null) {
             p.setPosition(pm.x, pm.y, pm.z);
             p.setYaw(pm.yaw);
             p.setPitch(pm.pitch);

             // Optionally, generate/send chunks if player moves near edge
             int playerChunkX = (int) Math.floor(p.getX() / Chunk.SIZE);
             int playerChunkZ = (int) Math.floor(p.getZ() / Chunk.SIZE);
             for (int cx = playerChunkX - 2; cx <= playerChunkX + 2; cx++) {
                 for (int cz = playerChunkZ - 2; cz <= playerChunkZ + 2; cz++) {
                     sendChunkIfNotSent(connection, cx, 0, cz);
                 }
             }
         }
     });

     packetHandlers.put(BlockUpdatePacket.class, (PacketHandler<BlockUpdatePacket>) (connection, bu) -> {
         Chunk chunk = world.getOrCreateChunk(bu.chunkX, bu.chunkY, bu.chunkZ, chunkGenerator, blockRegistry);
         if (chunk != null) {
             chunk.setBlock(bu.x, bu.y, bu.z, new Block(bu.blockType));
             // Optionally, send update to all clients who have this chunk
             // (not implemented here)
         }
     });

     // Add more handlers as needed!
 }
 
 public double getTPS() {
     return tps;
 }
}
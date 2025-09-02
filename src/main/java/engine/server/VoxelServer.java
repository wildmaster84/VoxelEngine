package engine.server;

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
import engine.common.world.FlatChunkGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public class VoxelServer {
 private NetworkManager network = new NetworkManager();
 private World world = new World(new File("serverworld"));
 private BlockRegistry blockRegistry = BlockRegistry.createDefault();
 private ChunkGenerator chunkGenerator = new FlatChunkGenerator();
 private Map<String, Player> players = new ConcurrentHashMap<>();

 // Track which chunks have been sent to which connection
 private Map<Connection, Set<String>> sentChunks = new ConcurrentHashMap<>();

 @FunctionalInterface
 interface PacketHandler<T> {
     void handle(Connection connection, T packet);
 }
 private final Map<Class<?>, PacketHandler<?>> packetHandlers = new HashMap<>();

 public static void main(String[] args) throws Exception {
     new VoxelServer().start();
 }

 public void start() throws Exception {
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
         System.out.println("Server: Sent chunk " + chunkKey + " to client.");
     } else {
         // Optional: System.out.println("Server: Skipped sending duplicate chunk " + chunkKey);
     }
 }

 private void registerPacketHandlers() {
     packetHandlers.put(PlayerJoinPacket.class, (PacketHandler<PlayerJoinPacket>) (connection, pj) -> {
         Player player = new Player();
         player.setPosition(pj.x, pj.y, pj.z);
         players.put(pj.playerId, player);

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
}
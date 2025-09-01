package engine.server;

import com.esotericsoftware.kryonet.*;
import engine.network.NetworkManager;
import engine.network.packet.*;
import engine.world.*;
import engine.block.BlockRegistry;
import engine.player.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoxelServer {
    private NetworkManager network = new NetworkManager();
    private World world = new World(new File("serverworld"));
    private BlockRegistry blockRegistry = BlockRegistry.createDefault();
    private ChunkGenerator chunkGenerator = new FlatChunkGenerator();
    private Map<String, Player> players = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        new VoxelServer().start();
    }

    public void start() throws Exception {
        network.startServer(54555, 54777);
        Server server = network.getServer();

        server.addListener(new Listener() {
            public void received(Connection connection, Object packet) {
                if(packet instanceof PlayerJoinPacket) {
                    PlayerJoinPacket pj = (PlayerJoinPacket)packet;
                    Player player = new Player();
                    player.setPosition(pj.x, pj.y, pj.z);
                    players.put(pj.playerId, player);

                    // Generate and send chunks around the player
                    int playerChunkX = (int)Math.floor(player.getX() / Chunk.SIZE);
                    int playerChunkZ = (int)Math.floor(player.getZ() / Chunk.SIZE);
                    for (int cx = playerChunkX - 2; cx <= playerChunkX + 2; cx++) {
                        for (int cz = playerChunkZ - 2; cz <= playerChunkZ + 2; cz++) {
                            Chunk chunk = getOrCreateChunk(cx, 0, cz);
                            ChunkDataPacket chunkPacket = new ChunkDataPacket(
                                cx, 0, cz,
                                chunk.serializeBlocks() // implement this method
                            );
                            connection.sendTCP(chunkPacket);
                        }
                    }
                }
                if(packet instanceof PlayerMovePacket) {
                    PlayerMovePacket pm = (PlayerMovePacket)packet;
                    Player player = players.get(pm.playerId);
                    if(player != null) {
                    	
                        player.setPosition(pm.x, pm.y, pm.z);
                        player.setYaw(pm.yaw);
                        player.setPitch(pm.pitch);

                        // Optionally, generate/send chunks if player moves near edge
                        int playerChunkX = (int)Math.floor(player.getX() / Chunk.SIZE);
                        int playerChunkZ = (int)Math.floor(player.getZ() / Chunk.SIZE);
                        for (int cx = playerChunkX - 2; cx <= playerChunkX + 2; cx++) {
                            for (int cz = playerChunkZ - 2; cz <= playerChunkZ + 2; cz++) {
                                Chunk chunk = getOrCreateChunk(cx, 0, cz);
                                ChunkDataPacket chunkPacket = new ChunkDataPacket(
                                    cx, 0, cz,
                                    chunk.serializeBlocks()
                                );
                                connection.sendTCP(chunkPacket);
                            }
                        }
                    }
                }
                if(packet instanceof BlockUpdatePacket) {
                    BlockUpdatePacket bu = (BlockUpdatePacket)packet;
                    Chunk chunk = getOrCreateChunk(bu.chunkX, bu.chunkY, bu.chunkZ);
                    chunk.setBlock(bu.x, bu.y, bu.z, new Block(bu.blockType));
                }
            }
        });
    }

    public Chunk getOrCreateChunk(int x, int y, int z) {
        Chunk chunk = world.getChunk(x, y, z);
        if (chunk == null) {
            chunk = new Chunk(x, y, z);
            chunkGenerator.generate(chunk, blockRegistry);
            world.setChunk(chunk);
            world.saveChunk(chunk);
        }
        return chunk;
    }
}
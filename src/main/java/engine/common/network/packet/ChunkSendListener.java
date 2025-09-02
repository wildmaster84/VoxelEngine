package engine.common.network.packet;

import com.esotericsoftware.kryonet.Connection;
import engine.common.world.Chunk;

public interface ChunkSendListener {
    void onSendChunk(Connection connection, Chunk chunk);
}
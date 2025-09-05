package engine.common.network;

import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.esotericsoftware.kryonet.*;

import engine.common.network.packet.BlockUpdatePacket;
import engine.common.network.packet.ChunkDataPacket;
import engine.common.network.packet.PlayerChatPacket;
import engine.common.network.packet.PlayerJoinPacket;
import engine.common.network.packet.PlayerMovePacket;

public class NetworkManager {
    private Server server;
    private Client client;
    public void registerPackets(EndPoint endPoint) {
        endPoint.getKryo().register(PlayerMovePacket.class);
        endPoint.getKryo().register(BlockUpdatePacket.class);
        endPoint.getKryo().register(PlayerJoinPacket.class);
        endPoint.getKryo().register(PlayerChatPacket.class);
        endPoint.getKryo().register(ChunkDataPacket.class);
        endPoint.getKryo().register(byte[].class); 
    }
    public void startServer(int tcpPort, int udpPort) throws Exception {
        server = new Server(8192, 8192);
        registerPackets(server);
        server.start();
        server.bind(tcpPort, udpPort);
    }
    public void startClient(String host, int tcpPort, int udpPort) throws Exception {
        client = new Client(1024, 1024);
        registerPackets(client);
        client.start();
        client.connect(5000, host, tcpPort, udpPort);
    }
    public Server getServer() { return server; }
    public Client getClient() { return client; }
    
    public static byte[] compress(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        byte[] buffer = new byte[1024];
        int compressedSize = deflater.deflate(buffer);
        byte[] output = new byte[compressedSize];
        System.arraycopy(buffer, 0, output, 0, compressedSize);
        return output;
    }

    public static byte[] decompress(byte[] data) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        byte[] buffer = new byte[4096];
        int decompressedSize = inflater.inflate(buffer);
        byte[] output = new byte[decompressedSize];
        System.arraycopy(buffer, 0, output, 0, decompressedSize);
        return output;
    }
}
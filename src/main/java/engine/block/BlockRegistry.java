package engine.block;

import java.util.*;

public class BlockRegistry {
    public static class BlockInfo {
        public final byte id;
        public final String name;
        public final float r, g, b;
        public BlockInfo(byte id, String name, float r, float g, float b) {
            this.id = id; this.name = name; this.r = r; this.g = g; this.b = b;
        }
    }
    private final Map<Byte, BlockInfo> idMap = new HashMap<>();
    private final Map<String, BlockInfo> nameMap = new HashMap<>();
    private byte airId = 0;
    public void register(byte id, String name, float r, float g, float b) {
        BlockInfo info = new BlockInfo(id, name, r, g, b);
        idMap.put(id, info);
        nameMap.put(name, info);
        if ("air".equals(name)) airId = id;
    }
    public BlockInfo getInfo(byte id) { return idMap.get(id); }
    public BlockInfo getInfo(String name) { return nameMap.get(name); }
    public byte getAirId() { return airId; }
    public static BlockRegistry createDefault() {
        BlockRegistry reg = new BlockRegistry();
        reg.register((byte)0, "air", 1f,1f,1f);
        reg.register((byte)1, "grass", 0.2f, 0.8f, 0.2f);
        reg.register((byte)2, "dirt", 0.6f, 0.4f, 0.2f);
        reg.register((byte)3, "stone", 0.5f, 0.5f, 0.5f);
        return reg;
    }
}
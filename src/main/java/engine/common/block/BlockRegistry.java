package engine.common.block;

import java.util.*;

public class BlockRegistry {
    public static class BlockInfo {
        public final byte id;
        public final String name;

        // New: per-face texture names
        public final String textureTop;
        public final String textureSide;
        public final String textureBottom;

        // Constructor for single-texture blocks
        public BlockInfo(byte id, String name, String texture) {
            this(id, name, texture, texture, texture);
        }
        // Constructor for multi-texture blocks
        public BlockInfo(byte id, String name, String textureTop, String textureSide, String textureBottom) {
            this.id = id;
            this.name = name;

            this.textureTop = textureTop;
            this.textureSide = textureSide;
            this.textureBottom = textureBottom;
        }
    }

    private final Map<Byte, BlockInfo> idMap = new HashMap<>();
    private final Map<String, BlockInfo> nameMap = new HashMap<>();
    private byte airId = 0;

    // Register block with single texture for all faces
    public void register(byte id, String name, String texture) {
        BlockInfo info = new BlockInfo(id, name, texture);
        idMap.put(id, info);
        nameMap.put(name, info);
        if ("air".equals(name)) airId = id;
    }
    // Register block with separate textures for top, side, bottom
    public void register(byte id, String name, String textureTop, String textureSide, String textureBottom) {
        BlockInfo info = new BlockInfo(id, name, textureTop, textureSide, textureBottom);
        idMap.put(id, info);
        nameMap.put(name, info);
        if ("air".equals(name)) airId = id;
    }

    public BlockInfo getInfo(byte id) { return idMap.get(id); }
    public BlockInfo getInfo(String name) { return nameMap.get(name); }
    public byte getAirId() { return airId; }

    public static BlockRegistry createDefault() {
        BlockRegistry reg = new BlockRegistry();
        // air: all faces use "air.png"
        reg.register((byte)0, "air", null);
        // grass: top "grass_top.png", sides "grass_side.png", bottom "dirt.png"
        reg.register((byte)1, "grass", "grass_top.png", "grass_side.png", "dirt.png");
        // dirt: all faces "dirt.png"
        reg.register((byte)2, "dirt", "dirt.png");
        // stone: all faces "stone.png"
        reg.register((byte)3, "stone", "stone.png");
        return reg;
    }
    public Collection<BlockInfo> getAllBlockInfos() {
        return idMap.values();
    }
}
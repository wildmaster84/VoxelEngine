package engine.plugin;

import engine.common.block.BlockRegistry;
import engine.common.player.Player;
import engine.common.world.Chunk;
import engine.server.World;

public interface EnginePlugin {
    default void onRegisterBlocks(BlockRegistry registry) {}
    default void onWorldInit(World world) {}
    default void onPlayerJoin(Player player) {}
    default void onPlayerLeave(Player player) {}
    default void onTick(float dt) {}
    default void onBlockPlaced(Player player, Chunk chunk, int x, int y, int z) {}
}
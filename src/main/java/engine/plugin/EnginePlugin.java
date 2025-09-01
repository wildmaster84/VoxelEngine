package engine.plugin;

import engine.block.BlockRegistry;
import engine.world.World;
import engine.player.Player;
import engine.world.Chunk;

public interface EnginePlugin {
    default void onRegisterBlocks(BlockRegistry registry) {}
    default void onWorldInit(World world) {}
    default void onPlayerJoin(Player player) {}
    default void onPlayerLeave(Player player) {}
    default void onTick(float dt) {}
    default void onBlockPlaced(Player player, Chunk chunk, int x, int y, int z) {}
}
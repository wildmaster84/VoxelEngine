package server.event.player;

import engine.common.player.Player;
import server.event.Cancellable;
import server.event.Event;

public class PlayerJoinEvent extends Event implements Cancellable {
    private final Player player;
    private boolean cancelled = false;

    public PlayerJoinEvent(Player player) {
        this.player = player;
    }
    
    public Player getPlayer() { return player; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
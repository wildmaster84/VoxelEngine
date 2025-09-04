package server.event.player;

import engine.common.player.Player;
import engine.plugin.Plugin;
import server.event.Cancellable;
import server.event.Event;

public class PlayerChatEvent extends Event implements Cancellable {
    private final Player player;
    private final String text;
    private final Plugin source;
    private boolean cancelled = false;

    public PlayerChatEvent(Player player, String text, Plugin source) {
        this.player = player;
        this.text = text;
        this.source = source;
    }

    public Player getPlayer() { return player; }
    public String getText() { return text; }
    public Plugin getSource() { return source; }

	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
		
	}
}
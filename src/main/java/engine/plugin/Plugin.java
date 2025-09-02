package engine.plugin;

import engine.server.Logger;
import server.Server;

public interface Plugin {
    void onEnable();
    void onDisable();
    String getName();
    default Logger getLogger() {
        return new Logger(getName());
    }
    default Server getServer() { return Server.getInstance();}
}
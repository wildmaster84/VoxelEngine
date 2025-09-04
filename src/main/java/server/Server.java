package server;

import engine.plugin.PluginLoader;
import engine.server.VoxelServer;
import server.event.*;

public class Server {
    
    private EventManager eventManager = new EventManager();
    private PluginLoader pluginLoader = new PluginLoader(eventManager);
    private static Server instance;
    private VoxelServer self = null;

    public void start(VoxelServer self) throws Exception {
    	if (self == null) return;
    	this.self = self;
    	
    	instance = this;
        pluginLoader.loadPlugins("./plugins");

        // Simulate player join
        //eventManager.fireEvent(new PlayerJoinEvent("wildmaster84"));

        // Main server loop example
        while (true) {
            // ... handle networking, tick game logic, etc
            // You can fire more events here
            Thread.sleep(1000); // Simulate tick
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }
    
    public PluginLoader getPluginManager() {
        return pluginLoader;
    }
    
    public static Server getInstance() {
    	return instance;
    }
    
    public VoxelServer getCraftServer() {
    	return self;
    }
}
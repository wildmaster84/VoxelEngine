package engine.plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import server.event.Event;
import server.event.EventManager;

public class PluginLoader {
    private List<Plugin> plugins = new ArrayList<>();
    private EventManager eventManager;
    
    public PluginLoader(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void loadPlugins(String pluginsDirectoryPath) throws Exception {
        File pluginDir = new File(pluginsDirectoryPath);
        if (!pluginDir.exists()) pluginDir.mkdirs();
        File[] jarFiles = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        for (File jar : jarFiles) {
            load(jar);
        }
        System.out.println("Loaded " + plugins.size() + " plugins ");
    }
    
    public Plugin load(File pluginJarFile) throws Exception {
        URL url = pluginJarFile.toURI().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[]{url}, getClass().getClassLoader());

        // Convention: plugin main class specified in a plugin.yml, or fallback to first found
        String mainClassName = PluginDescriptor.getMainClassName(pluginJarFile);
        if (mainClassName == null) {
        	loader.close();
            return null;
        }
        Class<?> clazz = loader.loadClass(mainClassName);
        Object pluginInstance = clazz.getDeclaredConstructor().newInstance();

        if (pluginInstance instanceof Plugin) {
        	loader.close();
        	Plugin plugin = (Plugin) pluginInstance;
            plugin.onEnable();
            registerAnnotatedListeners(plugin, eventManager);
            plugins.add(plugin);
            return plugin;
        }
        loader.close();
        return null;
    }

    public void disableAll() {
        for (Plugin plugin : plugins) {
            plugin.onDisable();
            eventManager.unregisterPluginListeners(plugin);
        }
    }
    
    public void disable(Plugin plugin) {
    	plugin.onDisable();
        eventManager.unregisterPluginListeners(plugin);
    }
    
    private void registerAnnotatedListeners(Plugin plugin, EventManager eventManager) {
        Class<?> pluginClass = plugin.getClass();
        for (Method method : pluginClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(server.event.EventHandler.class)) {
                // Must take exactly one argument: the Event type
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && Event.class.isAssignableFrom(params[0])) {
                    Class<? extends Event> eventType = params[0].asSubclass(Event.class);
                    method.setAccessible(true);
                    // Register listener
                    eventManager.registerListener(eventType, event -> {
                        try {
                            method.invoke(plugin, event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
    }
}
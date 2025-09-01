# Voxel Engine

A lightweight, modular, annotation-based plugin engine for Java servers.  
Inspired by Bukkit/Spigot, this engine provides a simple API for plugins, event handling (with cancellation), per-plugin logging, and dynamic plugin loading/unloading via JARs.  
Designed for extensibility and clarity.

---

## Features

- **Plugin Discovery & Loading:**  
  Plugins are packaged as JARs and auto-discovered from the `plugins/` folder.  
  Each plugin includes a `plugin.yml` specifying its main class and metadata.

- **Annotation-Based Event System:**  
  Event listeners are registered via `@EventHandler` annotations.  
  Supports event cancellation and automatic listener registration/unregistration.

- **Per-Plugin Logging:**  
  Plugins log directly to the console using `getLogger().info("...")` and related methods.

- **Plugin Enable/Disable:**  
  Plugins can be enabled/disabled at runtime, with all listeners cleaned up on disable.

- **Simple API, Easy to Extend:**  
  Designed to be understandable and hackable.

---

## Getting Started

### Prerequisites

- Java 8+ (tested with Java 11+)
- Basic knowledge of Java packaging

---

### 1. **Building the Engine**

Compile all files in the `server/` directory:

```sh
javac -d out/ $(find ./server -name "*.java")
```

---

### 2. **Running the Server**

Assuming your server entry point is `engine.server.VoxelServer`:

```sh
java -cp out engine.server.VoxelServer
```

### 2. **Running the Client**

Assuming your server entry point is `engine.client.VoxelClient`:

```sh
java -cp out engine.client.VoxelClient
```

Plugins are loaded from the `plugins/` directory.

---

### 4. **Creating a Plugin**

#### **1. Write your plugin class:**  
Implement `server.plugin.VoxelPlugin`:

```java
import server.plugin.VoxelPlugin;
import server.event.EventHandler;
import server.event.PlayerJoinEvent;

public class MyPlugin implements VoxelPlugin {
    @Override
    public void onEnable() {
        getLogger().info("MyPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MyPlugin disabled!");
    }

    @Override
    public String getName() {
        return "MyPlugin";
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        getLogger().info("Hello, " + event.playerName);
        if ("badguy".equals(event.playerName)) event.setCancelled(true);
    }
}
```

#### **2. Add a `plugin.yml` file to your plugin JAR:**

```yaml name=plugin.yml
name: MyPlugin
main: myplugin.MyPlugin
version: 1.0
author: wildmaster84
```

#### **3. Package your plugin:**

```sh
jar cf MyPlugin.jar -C path/to/classes/ . plugin.yml
```

Place `MyPlugin.jar` in the `plugins/` directory.

---

## API Overview

- **Plugin Interface:**  
  - `onEnable()` / `onDisable()`: Called when plugin is enabled/disabled.
  - `getName()`: Returns the plugin's name.
  - `getLogger()`: Returns a per-plugin logger (`Logger`).

- **Event System:**  
  - Annotate event handler methods with `@EventHandler`.
  - Events can be cancellable if they implement `Cancellable`.

- **Event Registration:**  
  - Done automatically by the engine via reflection.

- **Logging:**  
  - `getLogger().info("message")`, `.warn()`, `.error()`, `.debug()`.

---

## Advanced

- **Unregistering Listeners:**  
  Listeners registered by a plugin are automatically unregistered when the plugin is disabled.

- **Loading/Unloading Plugins Dynamically:**  
  Use `PluginLoader.loadPlugin(File pluginJarFile)` to load a single plugin at runtime.

- **Custom Events:**  
  Define your own event classes by extending `server.event.Event`.

---

## FAQ

**How do I cancel an event?**  
If the event implements `Cancellable`, call `event.setCancelled(true)` in your handler.

**How do I log from a plugin?**  
Use `getLogger().info("message")`.

**How are listeners registered?**  
Register by annotating methods with `@EventHandler`—no manual registration needed.

**How do I unload a plugin?**  
Call the plugin's `onDisable()` and the engine will unregister all its listeners.

---

## License

MIT

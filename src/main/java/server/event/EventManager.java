package server.event;

import java.util.*;

import engine.plugin.Plugin;

public class EventManager {
    private final Map<Class<? extends Event>, List<EventListener<? extends Event>>> listeners = new HashMap<>();
    private final Map<Plugin, List<RegisteredListener>> pluginListeners = new HashMap<>();
    
    private static class RegisteredListener {
        final Class<? extends Event> eventType;
        final EventListener<? extends Event> listener;

        RegisteredListener(Class<? extends Event> eventType, EventListener<? extends Event> listener) {
            this.eventType = eventType;
            this.listener = listener;
        }
    }
    
    public <T extends Event> void registerListener(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void fireEvent(T event) {
        List<EventListener<? extends Event>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener<? extends Event> listener : eventListeners) {
                ((EventListener<T>) listener).handle(event);
                if (event instanceof Cancellable && ((Cancellable)event).isCancelled()) {
                    break; // Stop processing if event was cancelled
                }
            }
        }
    }
    
    public void unregisterPluginListeners(Plugin plugin) {
        List<RegisteredListener> toRemove = pluginListeners.remove(plugin);
        if (toRemove != null) {
            for (RegisteredListener rl : toRemove) {
                List<EventListener<? extends Event>> eventTypeListeners = listeners.get(rl.eventType);
                if (eventTypeListeners != null) {
                    eventTypeListeners.remove(rl.listener);
                }
            }
        }
    }
}
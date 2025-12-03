package com.concurrent.engine.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DispatchingEventHandler implements EventHandler{
    private final Map<EventType, List<EventHandler>> handlersByType = new ConcurrentHashMap<>();

    public void registerHandler(EventType type, EventHandler handler){
        handlersByType.computeIfAbsent(type, a -> new CopyOnWriteArrayList<>()).add(handler);
    }

    @Override
    public void handle(Event event) {
        List<EventHandler> eventHandlers = handlersByType.get(event.getType());
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            return;
        }
        for (EventHandler handler: eventHandlers) {
            try {
                handler.handle(event);
            } catch (Exception e) {
                System.out.println("Error happened in DispatchingEventHandler while handling an event: " + event.getId());
            }
        }
    }
}

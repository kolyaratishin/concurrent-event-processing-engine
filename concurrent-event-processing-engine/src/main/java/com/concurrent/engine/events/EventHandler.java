package com.concurrent.engine.events;

public interface EventHandler {
    void handle(Event event) throws Exception;
}

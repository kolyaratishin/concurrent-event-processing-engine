package com.concurrent.engine.events;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Event {
    private final String id;
    private final long timestamp;
    private final EventType type;
    private final String payload;

    private Event(String id, long timestamp, EventType type, String payload) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.payload = payload;
    }

    public static Event of(EventType eventType, String payload)
    {
        return new Event(UUID.randomUUID().toString(), System.currentTimeMillis(), eventType, payload);
    }

}

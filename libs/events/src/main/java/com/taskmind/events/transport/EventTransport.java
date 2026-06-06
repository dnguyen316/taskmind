package com.taskmind.events.transport;

import com.taskmind.events.DomainEvent;

public interface EventTransport {
    String publish(String streamKey, DomainEvent event);

    long streamLength(String streamKey);
}

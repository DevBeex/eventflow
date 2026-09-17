package com.eventflow.notification.consumer;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class ConsumerControl {

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public boolean isStopped() {
        return stopped.get();
    }

    public void stop() {
        stopped.set(true);
    }

    public void reset() {
        stopped.set(false);
    }
}

package com.seconddisplay.models;

import java.util.ArrayList;
import java.util.List;

public class TouchFrame {
    public long timestampMs;
    public List<TouchEvent> events;

    public TouchFrame(long timestampMs) {
        this.timestampMs = timestampMs;
        this.events = new ArrayList<>();
    }

    public void addEvent(byte pointerId, byte action, float x, float y) {
        events.add(new TouchEvent(pointerId, action, x, y));
    }

    public int getEventCount() {
        return events.size();
    }
}

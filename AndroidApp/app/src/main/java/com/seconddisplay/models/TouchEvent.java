package com.seconddisplay.models;

public class TouchEvent {
    public static final byte ACTION_DOWN = 0;
    public static final byte ACTION_UP = 1;
    public static final byte ACTION_MOVE = 2;
    public static final byte ACTION_CANCEL = 3;

    public byte pointerId;
    public byte action;
    public float x;
    public float y;

    public TouchEvent(byte pointerId, byte action, float x, float y) {
        this.pointerId = pointerId;
        this.action = action;
        this.x = x;
        this.y = y;
    }
}

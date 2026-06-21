package com.seconddisplay.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.seconddisplay.models.TouchFrame;
import com.seconddisplay.network.TouchSender;

public class DisplaySurface extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "DisplaySurface";

    private TouchSender touchSender;
    private SurfaceReadyListener surfaceListener;
    private final SparseArray<Integer> pointerIds = new SparseArray<>();

    public interface SurfaceReadyListener {
        void onSurfaceReady(SurfaceHolder holder);
        void onSurfaceDestroyed();
    }

    public DisplaySurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public void setTouchSender(TouchSender sender) {
        this.touchSender = sender;
    }

    public void setSurfaceReadyListener(SurfaceReadyListener listener) {
        this.surfaceListener = listener;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "Surface created");
        if (surfaceListener != null) {
            surfaceListener.onSurfaceReady(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "Surface changed: " + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Surface destroyed");
        if (surfaceListener != null) {
            surfaceListener.onSurfaceDestroyed();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (touchSender == null) return true;

        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();
        long now = System.currentTimeMillis();

        TouchFrame frame = new TouchFrame(now);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int idx = event.getActionIndex();
                int id = event.getPointerId(idx);
                float x = event.getX(idx) / getWidth();
                float y = event.getY(idx) / getHeight();
                frame.addEvent((byte) id, TouchEvent.ACTION_DOWN, x, y);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < pointerCount; i++) {
                    int id = event.getPointerId(i);
                    float x = event.getX(i) / getWidth();
                    float y = event.getY(i) / getHeight();
                    frame.addEvent((byte) id, TouchEvent.ACTION_MOVE, x, y);
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                int idx = event.getActionIndex();
                int id = event.getPointerId(idx);
                float x = event.getX(idx) / getWidth();
                float y = event.getY(idx) / getHeight();
                frame.addEvent((byte) id, TouchEvent.ACTION_UP, x, y);
                break;
            }
        }

        if (frame.getEventCount() > 0 && touchSender != null) {
            touchSender.sendTouchFrame(frame);
        }

        return true;
    }
}

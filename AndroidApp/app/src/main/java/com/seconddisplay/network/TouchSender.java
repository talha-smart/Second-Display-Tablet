package com.seconddisplay.network;

import android.util.Log;

import com.seconddisplay.models.TouchEvent;
import com.seconddisplay.models.TouchFrame;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TouchSender {

    private static final String TAG = "TouchSender";

    private final String host;
    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final BlockingQueue<TouchFrame> pendingFrames = new LinkedBlockingQueue<>();
    private Socket socket;
    private DataOutputStream out;
    private Thread writerThread;
    private ConnectionListener listener;

    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
    }

    public TouchSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public void connect() {
        if (running.get()) return;

        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                socket.setTcpNoDelay(true);
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                running.set(true);

                writerThread = new Thread(this::writerLoop, "TouchSenderWriter");
                writerThread.setDaemon(true);
                writerThread.start();

                Log.i(TAG, "Connected to " + host + ":" + port);
                if (listener != null) listener.onConnected();

            } catch (IOException e) {
                Log.e(TAG, "Failed to connect to " + host + ":" + port, e);
                if (listener != null) listener.onDisconnected();
            }
        }, "TouchConnector").start();
    }

    public void sendTouchFrame(TouchFrame frame) {
        if (running.get()) {
            pendingFrames.offer(frame);
        }
    }

    private void writerLoop() {
        while (running.get()) {
            try {
                TouchFrame frame = pendingFrames.take();

                out.writeInt((int) frame.timestampMs);
                out.writeByte(frame.getEventCount());

                for (TouchEvent event : frame.events) {
                    out.writeByte(event.pointerId);
                    out.writeByte(event.action);
                    out.writeFloat(event.x);
                    out.writeFloat(event.y);
                }

                out.flush();

            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                Log.e(TAG, "Write error, disconnecting", e);
                disconnect();
                break;
            }
        }
    }

    public void disconnect() {
        running.set(false);
        if (out != null) {
            try { out.close(); } catch (IOException ignored) { }
        }
        if (socket != null && !socket.isClosed()) {
            try { socket.close(); } catch (IOException ignored) { }
        }
        if (writerThread != null) {
            try { writerThread.join(2000); } catch (InterruptedException ignored) { }
        }
        pendingFrames.clear();
        if (listener != null) listener.onDisconnected();
        Log.i(TAG, "Disconnected");
    }
}

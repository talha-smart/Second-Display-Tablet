package com.seconddisplay.network;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpReceiver {

    public interface FrameListener {
        void onFrameReceived(byte[] nalData);
    }

    private static final String TAG = "UdpReceiver";
    private static final int BUFFER_SIZE = 1500;
    private static final int HEADER_SIZE = 10;
    private static final long FRAME_TIMEOUT_MS = 500;

    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<Integer, FrameBuffer> pendingFrames = new ConcurrentHashMap<>();
    private FrameListener listener;
    private DatagramSocket socket;
    private Thread receiveThread;

    public UdpReceiver(int port) {
        this.port = port;
    }

    public void setFrameListener(FrameListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running.get()) return;
        running.set(true);

        receiveThread = new Thread(this::receiveLoop, "UdpReceiver");
        receiveThread.setDaemon(true);
        receiveThread.start();

        Log.i(TAG, "UDP receiver started on port " + port);
    }

    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (receiveThread != null) {
            try { receiveThread.join(2000); } catch (InterruptedException ignored) { }
        }
        pendingFrames.clear();
        Log.i(TAG, "UDP receiver stopped");
    }

    private void receiveLoop() {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(1000);
            byte[] buffer = new byte[BUFFER_SIZE];

            while (running.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    if (packet.getLength() < HEADER_SIZE) continue;

                    byte[] data = packet.getData();
                    int frameSeq = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                    int fragIdx = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                    int fragTotal = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
                    int nalSize = ((data[6] & 0xFF) << 24) |
                                  ((data[7] & 0xFF) << 16) |
                                  ((data[8] & 0xFF) << 8) |
                                  (data[9] & 0xFF);

                    int payloadLen = packet.getLength() - HEADER_SIZE;
                    if (payloadLen <= 0) continue;

                    byte[] payload = new byte[payloadLen];
                    System.arraycopy(data, HEADER_SIZE, payload, 0, payloadLen);

                    FrameBuffer fb = pendingFrames.get(frameSeq);
                    if (fb == null) {
                        fb = new FrameBuffer(fragTotal);
                        pendingFrames.put(frameSeq, fb);
                    }

                    fb.addFragment(fragIdx, payload);

                    if (fb.isComplete()) {
                        pendingFrames.remove(frameSeq);
                        byte[] nalData = fb.assemble();
                        if (listener != null) {
                            listener.onFrameReceived(nalData);
                        }
                    }

                    cleanupStaleFrames();

                } catch (java.net.SocketTimeoutException e) {
                    // timeout is expected, just loop
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                Log.e(TAG, "UDP receive loop error", e);
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void cleanupStaleFrames() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, FrameBuffer>> it = pendingFrames.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, FrameBuffer> entry = it.next();
            if (now - entry.getValue().createdAt > FRAME_TIMEOUT_MS) {
                it.remove();
            }
        }
    }

    private static class FrameBuffer {
        final int totalFragments;
        final boolean[] received;
        final byte[][] fragments;
        final long createdAt;

        FrameBuffer(int totalFragments) {
            this.totalFragments = totalFragments;
            this.received = new boolean[totalFragments];
            this.fragments = new byte[totalFragments][];
            this.createdAt = System.currentTimeMillis();
        }

        synchronized void addFragment(int index, byte[] data) {
            if (index < totalFragments && !received[index]) {
                fragments[index] = data;
                received[index] = true;
            }
        }

        synchronized boolean isComplete() {
            for (boolean b : received) {
                if (!b) return false;
            }
            return true;
        }

        synchronized byte[] assemble() {
            int totalSize = 0;
            for (byte[] frag : fragments) {
                totalSize += frag.length;
            }
            byte[] result = new byte[totalSize];
            int offset = 0;
            for (byte[] frag : fragments) {
                System.arraycopy(frag, 0, result, offset, frag.length);
                offset += frag.length;
            }
            return result;
        }
    }
}

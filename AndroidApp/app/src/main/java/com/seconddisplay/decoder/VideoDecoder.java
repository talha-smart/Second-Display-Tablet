package com.seconddisplay.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder {

    private static final String TAG = "VideoDecoder";
    private static final String MIME_TYPE = "video/avc";

    private MediaCodec codec;
    private boolean isRunning;

    public boolean init(Surface surface) {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, 1920, 1080);
            format.setInteger(MediaFormat.KEY_MAX_WIDTH, 1920);
            format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 1080);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 60);

            codec = MediaCodec.createDecoderByType(MIME_TYPE);
            codec.configure(format, surface, null, 0);
            codec.start();
            isRunning = true;

            Log.i(TAG, "Video decoder initialized");
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize decoder", e);
            return false;
        }
    }

    public void decodeFrame(byte[] nalData) {
        if (!isRunning || codec == null) return;

        try {
            int inIndex = codec.dequeueInputBuffer(10000);
            if (inIndex >= 0) {
                ByteBuffer inputBuffer = codec.getInputBuffer(inIndex);
                if (inputBuffer != null) {
                    inputBuffer.clear();
                    inputBuffer.put(nalData);
                    long pts = System.nanoTime() / 1000;
                    codec.queueInputBuffer(inIndex, 0, nalData.length, pts, 0);
                }
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outIndex = codec.dequeueOutputBuffer(info, 10000);
            while (outIndex >= 0) {
                codec.releaseOutputBuffer(outIndex, true);
                outIndex = codec.dequeueOutputBuffer(info, 0);
            }

        } catch (Exception e) {
            Log.e(TAG, "Decode error", e);
        }
    }

    public void release() {
        isRunning = false;
        if (codec != null) {
            try {
                codec.stop();
                codec.release();
            } catch (Exception e) {
                Log.e(TAG, "Release error", e);
            }
            codec = null;
        }
        Log.i(TAG, "Video decoder released");
    }
}

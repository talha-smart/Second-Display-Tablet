package com.seconddisplay;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.seconddisplay.decoder.VideoDecoder;
import com.seconddisplay.network.TouchSender;
import com.seconddisplay.network.UdpReceiver;
import com.seconddisplay.ui.DisplaySurface;

public class DisplayActivity extends AppCompatActivity {

    private static final int UDP_PORT = 8899;

    private String hostIp;
    private int touchPort;
    private DisplaySurface displaySurface;
    private TextView statusText;

    private UdpReceiver udpReceiver;
    private TouchSender touchSender;
    private VideoDecoder videoDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        hostIp = getIntent().getStringExtra("host");
        touchPort = getIntent().getIntExtra("port", 8890);

        displaySurface = findViewById(R.id.display_surface);
        statusText = findViewById(R.id.connection_status);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        videoDecoder = new VideoDecoder();

        udpReceiver = new UdpReceiver(UDP_PORT);
        udpReceiver.setFrameListener(nalData -> {
            runOnUiThread(() -> statusText.setVisibility(View.GONE));
            videoDecoder.decodeFrame(nalData);
        });

        touchSender = new TouchSender(hostIp, touchPort);
        touchSender.setConnectionListener(new TouchSender.ConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    statusText.setText("Connected");
                    statusText.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    statusText.setText("Disconnected");
                    statusText.setVisibility(View.VISIBLE);
                });
            }
        });

        displaySurface.setTouchSender(touchSender);
        displaySurface.setSurfaceReadyListener(new DisplaySurface.SurfaceReadyListener() {
            @Override
            public void onSurfaceReady(SurfaceHolder holder) {
                videoDecoder.init(holder.getSurface());
                udpReceiver.start();
                touchSender.connect();
            }

            @Override
            public void onSurfaceDestroyed() {
                videoDecoder.release();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (udpReceiver != null) udpReceiver.stop();
        if (touchSender != null) touchSender.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (udpReceiver != null) udpReceiver.stop();
        if (touchSender != null) touchSender.disconnect();
        if (videoDecoder != null) videoDecoder.release();
    }
}

package com.seconddisplay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SecondScreenPrefs";
    private static final String KEY_LAST_IP = "last_ip";
    private static final String KEY_LAST_PORT = "last_port";

    private EditText ipInput;
    private EditText portInput;
    private Button connectBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipInput = findViewById(R.id.ip_address);
        portInput = findViewById(R.id.port);
        connectBtn = findViewById(R.id.connect_btn);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ipInput.setText(prefs.getString(KEY_LAST_IP, ""));
        portInput.setText(String.valueOf(prefs.getInt(KEY_LAST_PORT, 8890)));

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ipInput.getText().toString().trim();
                String portStr = portInput.getText().toString().trim();

                if (ip.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter IP address", Toast.LENGTH_SHORT).show();
                    return;
                }

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid port", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit()
                    .putString(KEY_LAST_IP, ip)
                    .putInt(KEY_LAST_PORT, port)
                    .apply();

                Intent intent = new Intent(MainActivity.this, DisplayActivity.class);
                intent.putExtra("host", ip);
                intent.putExtra("port", port);
                startActivity(intent);
            }
        });
    }
}

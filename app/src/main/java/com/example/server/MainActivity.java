package com.example.server;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    Server server;
    TextView infoip, msg;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoip = findViewById(R.id.ip_info);
        msg = findViewById(R.id.msg);
        server = new Server(this, 9000);
        infoip.setText("Server listening at " + server.getIp() + ":" + server.getPort());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        server.onDestroy();
    }
}
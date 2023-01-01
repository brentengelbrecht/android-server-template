package com.example.server;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MyService extends Service {
    private MyServer myServer;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private final ArrayList<Messenger> mClients = new ArrayList<>();
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_VALUE = 3;

    public Consumer<String> onNotify = (s) -> {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, MSG_SET_VALUE, s));
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    };

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int serverPort = intent.getIntExtra("port", 9000);
        myServer = new MyServer(serverPort);
        myServer.onConnect = (ip, port) -> {
            if (onNotify != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onNotify.accept(String.format("Connection from %s:%d\n", ip, port));
                }
            }
            return true;
        };
        myServer.afterConnect = (connectionId) -> {
            if (onNotify != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onNotify.accept(String.format("Connected to client - id %s\n", connectionId));
                }
            }
        };
        myServer.afterWrite = (data) -> {
            if (onNotify != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onNotify.accept(String.format("Write op: %s\n", data));
                }
            }
        };
        myServer.afterRead = (data) -> {
            if (onNotify != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onNotify.accept(String.format("Read op: %s\n", data));
                }
            }
            return "exit";
        };
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            myServer.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}

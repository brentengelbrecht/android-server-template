package com.example.server;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    Messenger mService;
    boolean isBound;
    TextView infoip, msgLog;
    Button start, stop;

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyService.MSG_SET_VALUE:
                    msgLog.append("Received from service: " + msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            msgLog.append("Attached to service.\n");
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null, MyService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            msgLog.append("Detached from service.\n");
        }
    };

    void doBindService() {
        // Establish a connection with the service.
        bindService(new Intent(this, MyService.class), mConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
        msgLog.append("Binding.\n");
    }

    void doUnbindService() {
        if (isBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            isBound = false;
            msgLog.append("Unbinding.\n");
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoip = findViewById(R.id.ip_info);
        msgLog = findViewById(R.id.msg);
        start = findViewById(R.id.start_button);
        start.setOnClickListener(view -> {
            getApplicationContext().startService(new Intent(getApplicationContext(), MyService.class).putExtra("port", 9000));
            doBindService();
            start.setEnabled(false);
            stop.setEnabled(true);
        });
        stop = findViewById(R.id.stop_button);
        stop.setOnClickListener(view -> {
            doUnbindService();
            getApplicationContext().stopService(new Intent(getApplicationContext(), MyService.class));
            start.setEnabled(true);
            stop.setEnabled(false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

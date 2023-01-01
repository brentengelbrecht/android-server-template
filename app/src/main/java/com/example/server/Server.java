package com.example.server;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class Server {
    private final int serverPort;
    Thread socketServerThread;
    MainActivity activity;
    ServerSocket serverSocket;
    String ip = "";
    String message = "";

    public Server(MainActivity activity, int serverPort) {
        this.activity = activity;
        this.serverPort = serverPort;
        socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
        getIpAddress();
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return serverPort;
    }

    public void onDestroy() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getIpAddress() {
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();
                    if (inetAddress.isSiteLocalAddress()) {
                        ip = inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            ip += String.format("Something's wrong! %s\n", e);
        }
    }

    private class SocketServerThread extends Thread {
        int count = 0;
        boolean done = false;

        public void end() { done = true; }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(serverPort);
                while (!done) {
                    Socket socket = serverSocket.accept();
                    count++;
                    message += String.format("Connection from %s:%d\n", socket.getInetAddress(), socket.getPort());
                    activity.runOnUiThread(() -> activity.msg.setText(message));
                    SocketServerReplyThread socketServerReplyThread =
                            new SocketServerReplyThread(socket, String.format("%d-%d", getId(), count));
                    socketServerReplyThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SocketServerReplyThread extends Thread {
        private final Socket hostThreadSocket;
        private final String connId;

        SocketServerReplyThread(Socket socket, String connId) {
            hostThreadSocket = socket;
            this.connId = connId;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply = String.format("Hello from Server, you are #%s\n", connId);
            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msgReply);
                printStream.close();
                message += String.format("replied: %s\n", msgReply);
                activity.runOnUiThread(() -> activity.msg.setText(message));
            } catch (IOException e) {
                e.printStackTrace();
                message += String.format("Something's wrong! %s\n", e);
            }
            activity.runOnUiThread(() -> activity.msg.setText(message));
        }
    }
}

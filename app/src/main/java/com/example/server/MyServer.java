package com.example.server;

import android.annotation.SuppressLint;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class MyServer {
    private String ip = "";
    private final int serverPort;
    private final Thread socketServerThread;
    private ServerSocket serverSocket;

    public BiFunction<String, Integer, Boolean> onConnect;
    public Consumer<String> afterConnect;
    public Consumer<String> afterWrite;
    public Function<String, String> afterRead;

    public MyServer(int serverPort) {
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

    public void stop() throws InterruptedException {
        onDestroy();
    }

    public void onDestroy() throws InterruptedException {
        socketServerThread.interrupt();
        socketServerThread.join(1000);
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
        }
    }

    private class SocketServerThread extends Thread {
        int count = 0;

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(serverPort);
                while (!isInterrupted()) {
                    boolean onConnectResult = true;
                    Socket socket = serverSocket.accept();
                    incrementCount();
                    if (onConnect != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            onConnectResult = onConnect.apply(String.valueOf(socket.getInetAddress()), socket.getPort());
                        }
                    }
                    if (onConnectResult) {
                        String connId = String.format("%d-%d", getId(), count);
                        ClientConnectionThread clientConnectionThread = new ClientConnectionThread(socket, connId);
                        clientConnectionThread.start();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void incrementCount() {
            if (count < Integer.MAX_VALUE) {
                count++;
            } else {
                count = 1;
            }
        }
    }

    private class ClientConnectionThread extends Thread {
        private final Socket clientSocket;
        private final String connId;

        ClientConnectionThread(Socket clientSocket, String connId) {
            this.clientSocket = clientSocket;
            this.connId = connId;
            if (afterConnect != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    afterConnect.accept(String.valueOf(connId));
                }
            }
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            boolean done = false;
            String banner = String.format("Hello from Server, you are #%s\n", connId);
            PrintStream out = null;
            BufferedReader in = null;
            String toClient = banner;

            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintStream(clientSocket.getOutputStream());

                while (!done) {
                    out.print(toClient);
                    if (afterWrite != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            afterWrite.accept(String.format("%s: Replied with %d bytes\n", connId, banner.length()));
                        }
                    }

                    String fromClient = in.readLine();
                    if (afterRead != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            toClient = afterRead.apply(String.format("%s: Received %d bytes\n", connId, fromClient.length()));
                            done = "exit".equals(toClient);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    out.close();
                }
            }
        }
    }
}

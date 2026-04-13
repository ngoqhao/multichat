package com.mycompany.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClientService {

    public interface Listener {
        void onMessage(String msg);
        void onConnected();
        void onDisconnected();
        void onError(String err);
    }

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread readerThread;

    private Listener listener;

    public ChatClientService(Listener listener) {
        this.listener = listener;
    }

    public void connect(String ip, int port, String name, String pass) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

                // password flow
                String firstLine = in.readLine();
                if ("SYSTEM:PASSWORD_REQUIRED".equals(firstLine)) {
                    out.println(pass);
                    firstLine = in.readLine();
                    if (firstLine != null && firstLine.startsWith("SYSTEM:Sai mật khẩu")) {
                        listener.onError("Sai mật khẩu");
                        return;
                    }
                }

                out.println(name);
                listener.onConnected();

                if (firstLine != null) listener.onMessage(firstLine);

                readerThread = new Thread(() -> {
                    try {
                        String msg;
                        while ((msg = in.readLine()) != null) {
                            listener.onMessage(msg);
                        }
                    } catch (Exception e) {
                        listener.onError(e.getMessage());
                    }
                });
                readerThread.start();

            } catch (Exception e) {
                listener.onError(e.getMessage());
            }
        }).start();
    }

    public void send(String msg) {
        if (out != null) out.println(msg);
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
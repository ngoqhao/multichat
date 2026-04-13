package com.mycompany.test;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class ChatServerCore {

    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static ServerSocket serverSocket;
    private static ExecutorService executor;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    private static String serverRoomName = "Render Room";
    private static String serverPassword = "";
    private static String serverWelcome = "Welcome!";
    private static int serverMaxConn = 20;

    private static int totalJoined = 0;
    private static int totalMessages = 0;

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(System.getenv("PORT"));

            serverSocket = new ServerSocket(port);
            executor = Executors.newCachedThreadPool();

            System.out.println("[HT] Server running on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();

                if (clients.size() >= serverMaxConn) {
                    PrintWriter tmp = new PrintWriter(socket.getOutputStream(), true);
                    tmp.println("SYSTEM:Server full");
                    socket.close();
                    continue;
                }

                executor.execute(new ClientHandler(socket));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CLIENT HANDLER =================
    static class ClientHandler implements Runnable {

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private String ip;

        ClientHandler(Socket socket) {
            this.socket = socket;
            this.ip = socket.getInetAddress().getHostAddress();
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                // PASSWORD CHECK (giữ logic của bạn)
                if (!serverPassword.isEmpty()) {
                    out.println("SYSTEM:PASSWORD_REQUIRED");
                    String pass = in.readLine();
                    if (!serverPassword.equals(pass)) {
                        out.println("SYSTEM:Wrong password");
                        socket.close();
                        return;
                    }
                }

                out.println("SYSTEM:Enter username:");
                username = in.readLine();

                if (username == null || username.trim().isEmpty())
                    username = "Guest_" + (int)(Math.random() * 9999);

                username = username.trim();
                if (clients.containsKey(username))
                    username += "_" + (int)(Math.random() * 100);

                clients.put(username, this);
                totalJoined++;

                out.println("ROOMNAME:" + serverRoomName);
                out.println("SYSTEM:" + serverWelcome);
                out.println("SYSTEM:You are " + username);

                broadcastSystem(username + " joined (" + clients.size() + " online)");

                System.out.println("[JOIN] " + username + " - " + ip);

                String msg;
                while ((msg = in.readLine()) != null) {
                    msg = msg.trim();
                    if (msg.isEmpty()) continue;

                    if (msg.startsWith("/")) handleCmd(msg);
                    else broadcast(username + ": " + msg);
                }

            } catch (Exception e) {
                System.out.println("[ERROR] " + username);
            } finally {
                disconnect();
            }
        }

        void handleCmd(String cmd) {
            String[] p = cmd.split("\\s+", 2);

            switch (p[0]) {
                case "/list":
                    send("SYSTEM:Online: " + clients.keySet());
                    break;

                case "/quit":
                    disconnect();
                    break;

                default:
                    send("SYSTEM:Unknown command");
            }
        }

        void send(String msg) {
            if (out != null) out.println(msg);
        }

        void disconnect() {
            try {
                if (username != null) {
                    clients.remove(username);
                    broadcastSystem(username + " left");
                    System.out.println("[LEAVE] " + username);
                }
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    // ================= BROADCAST =================
    static void broadcast(String msg) {
        System.out.println(msg);
        totalMessages++;
        for (ClientHandler c : clients.values()) {
            c.send(msg);
        }
    }

    static void broadcastSystem(String msg) {
        String ts = SDF.format(new Date());
        String m = "[" + ts + "] " + msg;
        System.out.println(m);

        for (ClientHandler c : clients.values()) {
            c.send("SYSTEM:" + msg);
        }
    }
}
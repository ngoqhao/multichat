package com.mycompany.test;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatClient extends JFrame {

    // ====================== THEME (GIỮ NGUYÊN) ======================
    private static final Color BG_DARK = new Color(13, 15, 20);
    private static final Color BG_CARD = new Color(19, 22, 32);
    private static final Color BORDER_CLR = new Color(30, 34, 48);
    private static final Color TEXT_PRI = new Color(200, 205, 230);
    private static final Color TEXT_MUT = new Color(74, 80, 107);
    private static final Color ACCENT_BLU = new Color(79, 110, 247);
    private static final Color ACCENT_PUR = new Color(124, 92, 246);
    private static final Color GREEN_DOT = new Color(34, 197, 94);
    private static final Color RED_DOT = new Color(239, 68, 68);
    private static final Color BUBBLE_OWN = new Color(79, 110, 247);
    private static final Color BUBBLE_OTH = new Color(25, 28, 42);

    // ====================== STATE ======================
    private String username = "";
    private boolean connected = false;
    private boolean wasKicked = false;

    private ClientCore core; // 👈 TCP LAYER TÁCH RA

    // ====================== UI ======================
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private final DarkField ipField = new DarkField("localhost");
    private final DarkField portField = new DarkField("1234");
    private final DarkField nameField = new DarkField("Nhập tên...");
    private final DarkField passField = new DarkField("Mật khẩu (nếu có)");

    private final JPanel msgPanel = new JPanel();
    private JScrollPane scroll;
    private final DarkField msgInput = new DarkField("Nhập tin nhắn...");
    private final JLabel lblRoom = new JLabel("Phòng chung");
    private final JLabel lblStatus = new JLabel("Chưa kết nối");
    private JPanel statusDot;

    // ====================== MAIN ======================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }

    public ChatClient() {
        super("Chat Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 700);
        setLocationRelativeTo(null);

        mainPanel.add(buildConnectScreen(), "connect");
        mainPanel.add(buildChatScreen(), "chat");
        add(mainPanel);

        cardLayout.show(mainPanel, "connect");
        setVisible(true);
    }

    // ====================== CONNECT ======================
    private JPanel buildConnectScreen() {
        JPanel p = new JPanel(new GridLayout(5, 1, 10, 10));

        JButton btn = new JButton("Connect");
        btn.addActionListener(e -> doConnect());

        p.add(ipField);
        p.add(portField);
        p.add(nameField);
        p.add(passField);
        p.add(btn);

        return p;
    }

    // ====================== CHAT ======================
    private JPanel buildChatScreen() {
        JPanel p = new JPanel(new BorderLayout());

        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.Y_AXIS));
        scroll = new JScrollPane(msgPanel);

        JButton send = new JButton("Send");
        send.addActionListener(e -> doSend());

        p.add(scroll, BorderLayout.CENTER);
        p.add(msgInput, BorderLayout.SOUTH);
        p.add(send, BorderLayout.EAST);

        return p;
    }

    // ====================== CONNECT ACTION ======================
    private void doConnect() {
        String ip = ipField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        String name = nameField.getText().trim();
        String pass = passField.getText().trim();

        username = name;

        core = new ClientCore(ip, port);

        core.connect(pass, name,
            systemMsg -> SwingUtilities.invokeLater(() -> receiveMsg(systemMsg)),
            msg -> SwingUtilities.invokeLater(() -> receiveMsg(msg)),
            onConnected -> SwingUtilities.invokeLater(() -> {
                connected = true;
                cardLayout.show(mainPanel, "chat");
            }),
            onDisconnect -> SwingUtilities.invokeLater(() -> {
                connected = false;
                JOptionPane.showMessageDialog(this, "Disconnected");
                cardLayout.show(mainPanel, "connect");
            })
        );
    }

    private void doSend() {
        if (core == null) return;
        String text = msgInput.getText().trim();
        if (text.isEmpty()) return;

        core.send(text);
        msgInput.setText("");
    }

    // ====================== MESSAGE HANDLER ======================
    private void receiveMsg(String raw) {
        JLabel lbl = new JLabel(raw);
        lbl.setForeground(Color.WHITE);
        msgPanel.add(lbl);
        msgPanel.revalidate();
        msgPanel.repaint();
    }

    // ====================== CLIENT CORE (TCP - SERVERCORE STYLE) ======================
    static class ClientCore {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        private Thread listenThread;

        private final String host;
        private final int port;

        ClientCore(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public void connect(
                String pass,
                String name,
                java.util.function.Consumer<String> onSystem,
                java.util.function.Consumer<String> onMessage,
                java.util.function.Consumer<Void> onConnect,
                java.util.function.Consumer<Void> onDisconnect
        ) {
            new Thread(() -> {
                try {
                    socket = new Socket(host, port);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String first = in.readLine();
                    if (first != null && first.equals("SYSTEM:PASSWORD_REQUIRED")) {
                        out.println(pass);
                        first = in.readLine();
                        if (first != null && first.contains("Sai")) {
                            onSystem.accept("Sai mật khẩu");
                            return;
                        }
                    }

                    out.println(name);
                    onConnect.accept(null);

                    listenThread = new Thread(() -> {
                        try {
                            String line;
                            while ((line = in.readLine()) != null) {
                                if (line.startsWith("SYSTEM:")) onSystem.accept(line);
                                else onMessage.accept(line);
                            }
                        } catch (IOException e) {
                            onDisconnect.accept(null);
                        }
                    });

                    listenThread.start();

                } catch (Exception e) {
                    onSystem.accept("Không kết nối được server");
                }
            }).start();
        }

        public void send(String msg) {
            if (out != null) out.println(msg);
        }

        public void close() {
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        }
    }

    // ====================== UTIL ======================
    static class DarkField extends JTextField {
        DarkField(String ph) {
            super(ph);
        }
    }
}
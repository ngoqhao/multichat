package com.mycompany.test;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer extends JFrame {

    // ══════════════════════════════════════════════════════
    //  CONSTANTS & THEME
    // ══════════════════════════════════════════════════════
    private static final Color BG_MAIN   = new Color(245, 245, 248);
    private static final Color BG_CARD   = Color.WHITE;
    private static final Color BG_ACCENT = new Color(235, 233, 255);
    private static final Color C_PRIMARY = new Color(60, 52, 137);
    private static final Color C_SUCCESS = new Color(15, 110, 86);
    private static final Color C_DANGER  = new Color(163, 45, 45);
    private static final Color C_MUTED   = new Color(120, 120, 130);
    private static final Color C_BORDER  = new Color(220, 218, 235);
    private static final Color C_LOGBG   = new Color(22, 22, 32);
    private static final Color C_LOG_SYS  = new Color(170, 160, 255);
    private static final Color C_LOG_JOIN = new Color(80, 210, 150);
    private static final Color C_LOG_KICK = new Color(255, 120, 120);
    private static final Color C_LOG_MSG  = new Color(190, 190, 205);

    private static final Font F_TITLE = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font F_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_MONO  = new Font("Consolas", Font.PLAIN, 12);
    private static final Font F_STAT  = new Font("Segoe UI", Font.BOLD, 28);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    // ══════════════════════════════════════════════════════
    //  SERVER STATE
    // ══════════════════════════════════════════════════════
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private ServerSocket  serverSocket;
    private ExecutorService executor;
    private boolean running = false;

    private String serverRoomName = "";
    private String serverPassword = "";
    private String serverWelcome  = "";
    private int    serverPort     = 1234;
    private int    serverMaxConn  = 20;

    private int totalJoined   = 0;
    private int totalMessages = 0;

    // ══════════════════════════════════════════════════════
    //  GUI COMPONENTS
    // ══════════════════════════════════════════════════════
    private JTextField tfRoomName, tfPort, tfMaxConn, tfPassword, tfWelcome;
    private JButton    btnStart, btnStop;
    private JLabel     lblOpenStatus;
    private JPanel     dotOpen;

    private JLabel     lblOnline, lblTotal, lblMessages, lblManageStatus;
    private JPanel     dotManage;
    private JTable     userTable;
    private DefaultTableModel userModel;
    private JTextPane  logPane;
    private JButton    btnKick, btnBroadcast;
    private JTextField tfBroadcast;

    // ══════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ChatServer().setVisible(true));
    }

    // ══════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════
    public ChatServer() {
        super("Chat Server Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 660);
        setMinimumSize(new Dimension(700, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);
        buildUI();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { if (running) stopServer(); }
        });
    }

    // ══════════════════════════════════════════════════════
    //  BUILD UI
    // ══════════════════════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(F_TITLE);
        tabs.setBackground(BG_MAIN);
        tabs.addTab("  Mở phòng  ", buildOpenTab());
        tabs.addTab("  Quản lý phòng  ", buildManageTab());
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_PRIMARY);
        p.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel t1 = new JLabel("Chat Server Manager");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 18));
        t1.setForeground(Color.WHITE);
        JLabel t2 = new JLabel("Quản lý phòng chat TCP/IP");
        t2.setFont(F_SMALL);
        t2.setForeground(new Color(200, 190, 255));
        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        left.add(t1); left.add(t2);
        p.add(left, BorderLayout.WEST);
        return p;
    }

    // ──────────────────────────────────────────────────────
    //  TAB 1: MỞ PHÒNG
    // ──────────────────────────────────────────────────────
    private JPanel buildOpenTab() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        statusBar.setBackground(new Color(240, 238, 250));
        statusBar.setBorder(new LineBorder(C_BORDER, 1, true));
        statusBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        dotOpen = makeDot();
        lblOpenStatus = new JLabel("Server chưa khởi động");
        lblOpenStatus.setFont(F_BODY);
        lblOpenStatus.setForeground(C_MUTED);
        statusBar.add(dotOpen); statusBar.add(lblOpenStatus);
        root.add(statusBar);
        root.add(Box.createVerticalStrut(12));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(C_BORDER, 1, true), "  Cấu hình phòng chat  ",
            TitledBorder.LEFT, TitledBorder.TOP, F_TITLE, C_PRIMARY));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));

        GridBagConstraints g = gbc();

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        card.add(lbl("Tên phòng *"), g);
        g.gridy = 1;
        tfRoomName = field("VD: Phòng kỹ thuật, Nhóm dự án...");
        card.add(tfRoomName, g);

        g.gridy = 2; g.gridwidth = 1;
        card.add(lbl("Cổng (Port)"), g);
        g.gridx = 1; card.add(lbl("Số kết nối tối đa"), g);
        g.gridy = 3; g.gridx = 0;
        tfPort = field("1234"); card.add(tfPort, g);
        g.gridx = 1;
        tfMaxConn = field("20"); card.add(tfMaxConn, g);

        g.gridy = 4; g.gridx = 0; g.gridwidth = 2;
        card.add(lbl("Mật khẩu phòng (để trống nếu không cần)"), g);
        g.gridy = 5;
        tfPassword = field("Nhập mật khẩu...");
        card.add(tfPassword, g);

        g.gridy = 6; card.add(lbl("Tin nhắn chào mừng"), g);
        g.gridy = 7;
        tfWelcome = field("VD: Chào mừng đến phòng chat!");
        card.add(tfWelcome, g);

        g.gridy = 8; card.add(lbl("Địa chỉ IP của máy chủ"), g);
        g.gridy = 9;
        JTextArea ipBox = new JTextArea(getLocalIPs());
        ipBox.setEditable(false);
        ipBox.setFont(F_MONO);
        ipBox.setBackground(BG_ACCENT);
        ipBox.setForeground(C_PRIMARY);
        ipBox.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        ipBox.setRows(2);
        card.add(ipBox, g);

        root.add(card);
        root.add(Box.createVerticalStrut(14));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        btnStart = actionBtn("▶   Khởi động Server", C_PRIMARY, Color.WHITE);
        btnStart.setPreferredSize(new Dimension(210, 40));
        btnStart.addActionListener(e -> doStart());

        btnStop = actionBtn("■   Dừng Server", C_DANGER, Color.WHITE);
        btnStop.setPreferredSize(new Dimension(210, 40));
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> doStop());

        btnRow.add(btnStart); btnRow.add(btnStop);
        root.add(btnRow);
        return root;
    }

    // ──────────────────────────────────────────────────────
    //  TAB 2: QUẢN LÝ
    // ──────────────────────────────────────────────────────
    private JPanel buildManageTab() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        JPanel mStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        mStatus.setBackground(new Color(240, 238, 250));
        mStatus.setBorder(new LineBorder(C_BORDER, 1, true));
        mStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        dotManage = makeDot();
        lblManageStatus = new JLabel("Server chưa khởi động");
        lblManageStatus.setFont(F_BODY);
        lblManageStatus.setForeground(C_MUTED);
        mStatus.add(dotManage); mStatus.add(lblManageStatus);

        top.add(mStatus);
        top.add(Box.createVerticalStrut(10));
        top.add(buildStatCards());
        root.add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildUserPanel(), buildLogPanel());
        split.setDividerLocation(350);
        split.setBorder(null);
        split.setBackground(BG_MAIN);
        root.add(split, BorderLayout.CENTER);

        root.add(buildBroadcastBar(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildStatCards() {
        JPanel p = new JPanel(new GridLayout(1, 3, 12, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
        lblOnline   = new JLabel("0", SwingConstants.CENTER);
        lblTotal    = new JLabel("0", SwingConstants.CENTER);
        lblMessages = new JLabel("0", SwingConstants.CENTER);
        p.add(statCard("Đang online",   lblOnline,   C_SUCCESS));
        p.add(statCard("Tổng lượt vào", lblTotal,    C_PRIMARY));
        p.add(statCard("Tin nhắn",      lblMessages, new Color(160, 100, 0)));
        return p;
    }

    private JPanel statCard(String label, JLabel val, Color color) {
        JPanel c = new JPanel(new BorderLayout(0, 4));
        c.setBackground(BG_CARD);
        c.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        val.setFont(F_STAT);
        val.setForeground(color);
        JLabel l = new JLabel(label, SwingConstants.CENTER);
        l.setFont(F_SMALL); l.setForeground(C_MUTED);
        c.add(l, BorderLayout.NORTH);
        c.add(val, BorderLayout.CENTER);
        return c;
    }

    private JPanel buildUserPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JLabel title = new JLabel("Người tham gia");
        title.setFont(F_TITLE); title.setForeground(C_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        userModel = new DefaultTableModel(new String[]{"Tên người dùng", "Địa chỉ IP", "Vào lúc"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(userModel);
        userTable.setFont(F_BODY);
        userTable.setRowHeight(28);
        userTable.setSelectionBackground(BG_ACCENT);
        userTable.setSelectionForeground(C_PRIMARY);
        userTable.setGridColor(C_BORDER);
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        userTable.getTableHeader().setBackground(new Color(235, 233, 250));
        userTable.getTableHeader().setForeground(C_PRIMARY);
        userTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(new LineBorder(C_BORDER, 1, true));
        btnKick = actionBtn("Kick người dùng", C_DANGER, Color.WHITE);
        btnKick.setEnabled(false);
        btnKick.addActionListener(e -> doKick());
        userTable.getSelectionModel().addListSelectionListener(e ->
            btnKick.setEnabled(userTable.getSelectedRow() >= 0 && running));
        p.add(title, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        p.add(btnKick, BorderLayout.SOUTH);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        return p;
    }

    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JLabel title = new JLabel("Nhật ký hệ thống");
        title.setFont(F_TITLE); title.setForeground(C_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(C_LOGBG);
        logPane.setFont(F_MONO);
        JScrollPane scroll = new JScrollPane(logPane);
        scroll.setBorder(new LineBorder(C_BORDER, 1, true));
        JButton btnClear = actionBtn("Xoá log", new Color(60, 60, 75), Color.WHITE);
        btnClear.setFont(F_SMALL);
        btnClear.addActionListener(e -> logPane.setText(""));
        p.add(title, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        p.add(btnClear, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildBroadcastBar() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel lbl = new JLabel("Thông báo toàn phòng:");
        lbl.setFont(F_BODY); lbl.setForeground(C_MUTED);
        tfBroadcast = new JTextField();
        tfBroadcast.setFont(F_BODY);
        tfBroadcast.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tfBroadcast.addActionListener(e -> doBroadcast());
        btnBroadcast = actionBtn("Gửi thông báo", C_PRIMARY, Color.WHITE);
        btnBroadcast.setEnabled(false);
        btnBroadcast.addActionListener(e -> doBroadcast());
        p.add(lbl, BorderLayout.WEST);
        p.add(tfBroadcast, BorderLayout.CENTER);
        p.add(btnBroadcast, BorderLayout.EAST);
        return p;
    }

    // ══════════════════════════════════════════════════════
    //  SERVER ACTIONS
    // ══════════════════════════════════════════════════════
    private void doStart() {
        String name = tfRoomName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập tên phòng!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Cổng không hợp lệ (1–65535)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int maxConn;
        try { maxConn = Integer.parseInt(tfMaxConn.getText().trim()); }
        catch (Exception ex) { maxConn = 20; }

        serverRoomName = name;
        serverPort     = port;
        serverMaxConn  = maxConn;
        serverPassword = tfPassword.getText().trim();
        serverWelcome  = tfWelcome.getText().trim();
        if (serverWelcome.isEmpty()) serverWelcome = "Chào mừng đến " + serverRoomName + "!";

        totalJoined = 0; totalMessages = 0;

        try {
            serverSocket = new ServerSocket(serverPort);
            serverSocket.setReuseAddress(true);
            running = true;
            executor = Executors.newCachedThreadPool();

            log("[HT] Server \"" + serverRoomName + "\" khởi động trên cổng " + serverPort, C_LOG_SYS);
            setRunningUI(true);

            Thread accept = new Thread(() -> {
                while (running) {
                    try {
                        Socket s = serverSocket.accept();
                        if (clients.size() >= serverMaxConn) {
                            PrintWriter tmp = new PrintWriter(
                                new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
                            tmp.println("SYSTEM:Server đã đầy (" + serverMaxConn + " kết nối).");
                            s.close();
                            log("[HT] Từ chối kết nối (đã đầy): " + s.getInetAddress().getHostAddress(), C_LOG_SYS);
                            continue;
                        }
                        log("[KN] Kết nối mới: " + s.getInetAddress().getHostAddress(), C_LOG_SYS);
                        executor.execute(new ClientHandler(s));
                    } catch (IOException e) {
                        if (running) log("[LỖI] " + e.getMessage(), C_LOG_KICK);
                    }
                }
            });
            accept.setDaemon(true);
            accept.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Không thể khởi động:\n" + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            running = false;
        }
    }

    private void doStop() {
        int ok = JOptionPane.showConfirmDialog(this,
            "Dừng server sẽ ngắt kết nối tất cả người dùng.\nBạn có chắc?",
            "Xác nhận dừng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        new Thread(this::stopServer).start();
    }

    private void stopServer() {
        running = false;
        broadcastSystem("Server đang đóng...");
        for (ClientHandler ch : clients.values()) ch.close();
        clients.clear();
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        if (executor != null) executor.shutdownNow();
        SwingUtilities.invokeLater(() -> {
            log("[HT] Server đã dừng.", C_LOG_SYS);
            setRunningUI(false);
            refreshTable();
            refreshStats();
        });
    }

    private void doKick() {
        int row = userTable.getSelectedRow();
        if (row < 0) return;
        String uname = (String) userModel.getValueAt(row, 0);
        int ok = JOptionPane.showConfirmDialog(this,
            "Kick \"" + uname + "\" khỏi phòng?",
            "Xác nhận Kick", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        ClientHandler ch = clients.get(uname);
        if (ch != null) {
            ch.sendMsg("SYSTEM:Bạn đã bị kick bởi Admin.");
            ch.close();
            clients.remove(uname);
            broadcastSystem(uname + " đã bị Admin kick khỏi phòng.");
            log("[KICK] " + uname + " đã bị kick.", C_LOG_KICK);
            refreshTable();
            refreshStats();
        }
    }

    private void doBroadcast() {
        String msg = tfBroadcast.getText().trim();
        if (!msg.isEmpty() && running) {
            broadcastSystem("Admin: " + msg);
            tfBroadcast.setText("");
        }
    }

    // ══════════════════════════════════════════════════════
    //  SERVER HELPERS
    // ══════════════════════════════════════════════════════
    private void broadcast(String message, String sender) {
        String ts = SDF.format(new Date());
        String formatted = "[" + ts + "] " + sender + ": " + message;
        log(formatted, C_LOG_MSG);
        totalMessages++;
        for (ClientHandler ch : clients.values()) ch.sendMsg(formatted);
        SwingUtilities.invokeLater(this::refreshStats);
    }

    private void broadcastSystem(String message) {
        String ts = SDF.format(new Date());
        log("[" + ts + "] *** " + message + " ***", C_LOG_SYS);
        for (ClientHandler ch : clients.values()) ch.sendMsg("SYSTEM:" + message);
    }

    /**
     * Trả về chuỗi "Online (N): name1, name2, ..." — client parse được số lượng và tên.
     */
    private String getUserList() {
        if (clients.isEmpty()) return "Online (0): (trống)";
        return "Online (" + clients.size() + "): " + String.join(", ", clients.keySet());
    }

    // ══════════════════════════════════════════════════════
    //  CLIENT HANDLER
    // ══════════════════════════════════════════════════════
    private class ClientHandler implements Runnable {
        private final Socket   socket;
        private final String   ip;
        private final String   joinedAt;
        private PrintWriter    out;
        private BufferedReader in;
        private String         username;

        ClientHandler(Socket socket) {
            this.socket   = socket;
            this.ip       = socket.getInetAddress().getHostAddress();
            this.joinedAt = SDF.format(new Date());
        }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                // ── Bước 1: xác thực mật khẩu (nếu có) ──
                if (!serverPassword.isEmpty()) {
                    out.println("SYSTEM:PASSWORD_REQUIRED");
                    String pass = in.readLine();
                    if (pass == null || !pass.trim().equals(serverPassword)) {
                        out.println("SYSTEM:Sai mật khẩu. Kết nối bị từ chối.");
                        socket.close();
                        log("[MK] Sai mật khẩu từ: " + ip, C_LOG_KICK);
                        return;
                    }
                    // Mật khẩu đúng — gửi prompt username bình thường
                    out.println("SYSTEM:Mật khẩu đúng. Nhập tên của bạn:");
                }

                // ── Bước 2: nhận tên người dùng ──
                // (nếu không có mật khẩu, đây là dòng đầu tiên server gửi)
                if (serverPassword.isEmpty()) {
                    out.println("SYSTEM:Nhập tên của bạn:");
                }

                username = in.readLine();
                if (username == null || username.trim().isEmpty())
                    username = "Guest_" + (int)(Math.random() * 9000 + 1000);
                username = username.trim();
                if (clients.containsKey(username))
                    username = username + "_" + (int)(Math.random() * 100);

                // ── Bước 3: đăng ký và gửi thông tin phòng ──
                clients.put(username, this);
                totalJoined++;

                // Gửi tên phòng — client sẽ hiển thị trên header
                out.println("ROOMNAME:" + serverRoomName);

                // Gửi lời chào mừng và thông tin người dùng
                out.println("SYSTEM:" + serverWelcome);
                out.println("SYSTEM:Tên của bạn: " + username);

                // Thông báo toàn phòng về thành viên mới
                broadcastSystem(username + " đã tham gia! " + getUserList());
                log("[VÀO] " + username + " (" + ip + ")", C_LOG_JOIN);
                SwingUtilities.invokeLater(() -> { refreshTable(); refreshStats(); });

                // ── Bước 4: vòng lặp đọc tin nhắn ──
                String msg;
                while ((msg = in.readLine()) != null) {
                    msg = msg.trim();
                    if (msg.isEmpty()) continue;
                    if (msg.startsWith("/")) handleCmd(msg);
                    else broadcast(msg, username);
                }

            } catch (IOException e) {
                if (username != null)
                    log("[MKN] " + username + ": " + e.getMessage(), C_LOG_KICK);
            } finally {
                disconnect();
            }
        }

        private void handleCmd(String cmd) {
            String[] parts = cmd.split("\\s+", 2);
            switch (parts[0].toLowerCase()) {
                case "/quit": case "/exit":
                    out.println("SYSTEM:Tạm biệt " + username + "!");
                    disconnect(); break;
                case "/list":
                    out.println("SYSTEM:" + getUserList()); break;
                case "/pm":
                    if (parts.length < 2) { out.println("SYSTEM:Dùng: /pm <tên> <tin>"); break; }
                    String[] pm = parts[1].split("\\s+", 2);
                    if (pm.length < 2) { out.println("SYSTEM:Dùng: /pm <tên> <tin>"); break; }
                    ClientHandler target = clients.get(pm[0]);
                    if (target == null) { out.println("SYSTEM:Không tìm thấy: " + pm[0]); break; }
                    String ts = SDF.format(new Date());
                    target.sendMsg("[" + ts + "] [PM từ " + username + "]: " + pm[1]);
                    out.println("[" + ts + "] [PM gửi " + pm[0] + "]: " + pm[1]); break;
                case "/help":
                    out.println("SYSTEM:/list  /pm <tên> <tin>  /quit  /help"); break;
                default:
                    out.println("SYSTEM:Lệnh không hợp lệ. Gõ /help.");
            }
        }

        void sendMsg(String msg) { if (out != null) out.println(msg); }

        void close() {
            try { if (socket != null && !socket.isClosed()) socket.close(); }
            catch (IOException ignored) {}
        }

        private void disconnect() {
            if (username != null) {
                clients.remove(username);
                broadcastSystem(username + " đã rời phòng. " + getUserList());
                log("[RA] " + username + " đã rời phòng.", C_LOG_JOIN);
                username = null;
                SwingUtilities.invokeLater(() -> { refreshTable(); refreshStats(); });
            }
            close();
        }

        String getIp()       { return ip; }
        String getJoinedAt() { return joinedAt; }
        String getUsername() { return username; }
    }

    // ══════════════════════════════════════════════════════
    //  UI UPDATE HELPERS
    // ══════════════════════════════════════════════════════
    private void setRunningUI(boolean on) {
        String statusText = on
            ? "\"" + serverRoomName + "\" đang chạy · Port " + serverPort
            : "Server đã dừng";
        Color statusColor = on ? C_SUCCESS : C_MUTED;
        setDot(dotOpen,   on); lblOpenStatus.setText(statusText);   lblOpenStatus.setForeground(statusColor);
        setDot(dotManage, on); lblManageStatus.setText(statusText); lblManageStatus.setForeground(statusColor);
        btnStart.setEnabled(!on);
        btnStop.setEnabled(on);
        btnBroadcast.setEnabled(on);
        tfRoomName.setEnabled(!on); tfPort.setEnabled(!on);
        tfMaxConn.setEnabled(!on); tfPassword.setEnabled(!on); tfWelcome.setEnabled(!on);
    }

    private void refreshTable() {
        userModel.setRowCount(0);
        for (ClientHandler ch : clients.values()) {
            if (ch.getUsername() != null)
                userModel.addRow(new Object[]{ ch.getUsername(), ch.getIp(), ch.getJoinedAt() });
        }
        btnKick.setEnabled(false);
    }

    private void refreshStats() {
        lblOnline.setText(String.valueOf(clients.size()));
        lblTotal.setText(String.valueOf(totalJoined));
        lblMessages.setText(String.valueOf(totalMessages));
    }

    private void log(String raw, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = logPane.getStyledDocument();
            Style style = logPane.addStyle("s", null);
            StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), raw + "\n", style);
                logPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    // ══════════════════════════════════════════════════════
    //  UI COMPONENT BUILDERS
    // ══════════════════════════════════════════════════════
    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_SMALL); l.setForeground(C_MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
        return l;
    }

    private JTextField field(String tip) {
        JTextField f = new JTextField();
        f.setFont(F_BODY); f.setToolTipText(tip);
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return f;
    }

    private JButton actionBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        Color hover = bg.brighter();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(hover); }
            public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }

    private JPanel makeDot() {
        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Boolean.TRUE.equals(getClientProperty("on")) ? C_SUCCESS : C_MUTED);
                g.fillOval(0, 0, 10, 10);
            }
        };
        dot.setPreferredSize(new Dimension(10, 10));
        dot.setOpaque(false);
        dot.putClientProperty("on", false);
        return dot;
    }

    private void setDot(JPanel dot, boolean on) {
        dot.putClientProperty("on", on);
        dot.repaint();
    }

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 8, 2, 8);
        g.weightx = 1.0;
        return g;
    }

    private String getLocalIPs() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address)
                        sb.append(a.getHostAddress())
                          .append("   (").append(ni.getDisplayName()).append(")\n");
                }
            }
        } catch (Exception e) { sb.append("Không lấy được IP"); }
        return sb.toString().trim();
    }
}
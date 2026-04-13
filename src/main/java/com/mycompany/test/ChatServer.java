package com.mycompany.test;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer extends JFrame {

    // ═══════════════════════════════════════
    // THEME (GIỮ NGUYÊN)
    // ═══════════════════════════════════════
    private static final Color BG_MAIN   = new Color(245, 245, 248);
    private static final Color BG_CARD   = Color.WHITE;
    private static final Color BG_ACCENT = new Color(235, 233, 255);
    private static final Color C_PRIMARY = new Color(60, 52, 137);
    private static final Color C_SUCCESS = new Color(15, 110, 86);
    private static final Color C_DANGER  = new Color(163, 45, 45);
    private static final Color C_MUTED   = new Color(120, 120, 130);
    private static final Color C_BORDER  = new Color(220, 218, 235);
    private static final Color C_LOGBG   = new Color(22, 22, 32);

    private static final Font F_TITLE = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font F_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_MONO  = new Font("Consolas", Font.PLAIN, 12);
    private static final Font F_STAT  = new Font("Segoe UI", Font.BOLD, 28);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    // ═══════════════════════════════════════
    // UI STATE ONLY (KHÔNG SERVER)
    // ═══════════════════════════════════════
    private JLabel lblOpenStatus, lblManageStatus;
    private JPanel dotOpen, dotManage;

    private JLabel lblOnline, lblTotal, lblMessages;

    private JTable userTable;
    private DefaultTableModel userModel;

    private JTextPane logPane;

    private JButton btnKick, btnBroadcast;
    private JTextField tfBroadcast;

    // fake stats (UI only)
    private int online = 0;
    private int totalJoin = 0;
    private int totalMsg = 0;

    // ═══════════════════════════════════════
    // MAIN
    // ═══════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatServer().setVisible(true));
    }

    // ═══════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════
    public ChatServer() {
        setTitle("Chat Server Manager (UI Only)");
        setSize(780, 660);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
    }

    // ═══════════════════════════════════════
    // UI BUILD
    // ═══════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Mở phòng", buildOpenTab());
        tabs.addTab("Quản lý", buildManageTab());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_PRIMARY);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel t = new JLabel("Chat Server Manager (UI Only)");
        t.setForeground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.BOLD, 18));

        p.add(t, BorderLayout.WEST);
        return p;
    }

    // ═══════════════════════════════════════
    // TAB 1
    // ═══════════════════════════════════════
    private JPanel buildOpenTab() {

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        JLabel info = new JLabel("⚠ Server đã tách sang Render (ChatServerCore)");
        info.setForeground(Color.RED);

        JButton btn = new JButton("Test UI Only");
        btn.addActionListener(e -> log("UI chạy OK (không có server socket)", Color.BLUE));

        root.add(info);
        root.add(btn);

        return root;
    }

    // ═══════════════════════════════════════
    // TAB 2 (GIỮ UI FULL NHƯ BẠN)
    // ═══════════════════════════════════════
    private JPanel buildManageTab() {

        JPanel root = new JPanel(new BorderLayout());

        // stats
        JPanel stats = new JPanel(new GridLayout(1, 3));
        lblOnline = new JLabel("0", SwingConstants.CENTER);
        lblTotal = new JLabel("0", SwingConstants.CENTER);
        lblMessages = new JLabel("0", SwingConstants.CENTER);

        stats.add(card("Online", lblOnline));
        stats.add(card("Joined", lblTotal));
        stats.add(card("Messages", lblMessages));

        root.add(stats, BorderLayout.NORTH);

        // users
        userModel = new DefaultTableModel(new String[]{"User", "IP", "Time"}, 0);
        userTable = new JTable(userModel);

        root.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // bottom
        JPanel bottom = new JPanel(new BorderLayout());

        tfBroadcast = new JTextField();
        btnBroadcast = new JButton("Broadcast");
        btnBroadcast.addActionListener(e -> log("Broadcast (UI only)", Color.MAGENTA));

        bottom.add(tfBroadcast, BorderLayout.CENTER);
        bottom.add(btnBroadcast, BorderLayout.EAST);

        root.add(bottom, BorderLayout.SOUTH);

        // log
        logPane = new JTextPane();
        root.add(new JScrollPane(logPane), BorderLayout.EAST);

        return root;
    }

    private JPanel card(String title, JLabel val) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        val.setFont(F_STAT);
        p.add(t, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    // ═══════════════════════════════════════
    // LOG ONLY
    // ═══════════════════════════════════════
    private void log(String msg, Color c) {
        try {
            StyledDocument doc = logPane.getStyledDocument();
            Style s = logPane.addStyle("s", null);
            StyleConstants.setForeground(s, c);
            doc.insertString(doc.getLength(),
                "[" + SDF.format(new Date()) + "] " + msg + "\n", s);
        } catch (Exception ignored) {}
    }
}
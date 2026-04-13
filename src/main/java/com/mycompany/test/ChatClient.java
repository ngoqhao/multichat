package com.mycompany.test;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import static java.awt.Component.CENTER_ALIGNMENT;
import static java.awt.Component.LEFT_ALIGNMENT;
import static java.awt.Component.RIGHT_ALIGNMENT;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class ChatClient extends JFrame {

    // ====================== THEME ======================
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
    private Socket socket;
    private PrintWriter sockOut;
    private boolean connected = false;
    private boolean wasKicked = false;

    // Kích thước cửa sổ
    private static final Dimension CONNECT_SIZE = new Dimension(460, 700);
    private static final Dimension CHAT_SIZE    = new Dimension(800, 820);

    // ====================== LAYOUT ======================
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    // Màn hình kết nối
    private final DarkField ipField = new DarkField("localhost");
    private final DarkField portField = new DarkField("1234");
    private final DarkField nameField = new DarkField("Nhập tên...");
    private final DarkField passField = new DarkField("Mật khẩu (nếu có)");

    // Màn hình chat
    private final JPanel msgPanel = new JPanel();
    private JScrollPane scroll;
    private final DarkField msgInput = new DarkField("Nhập tin nhắn...");
    private final JLabel lblRoom = new JLabel("Phòng chung");
    private final JLabel lblStatus = new JLabel("Chưa kết nối");
    private JPanel statusDot;

    // Member panel
    private JPanel memberSidePanel;
    private JPanel memberListContainer;
    private JLabel lblOnlineCount;
    private boolean memberPanelVisible = false;
    private JButton btnToggleMembers;

    // ====================== MAIN ======================
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(ChatClient::new);
    }

    public ChatClient() {
        super("Chat Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(390, 580));
        setLocationRelativeTo(null);

        mainPanel.setBackground(BG_DARK);
        mainPanel.add(buildConnectScreen(), "connect");
        mainPanel.add(buildChatScreen(), "chat");
        add(mainPanel);

        cardLayout.show(mainPanel, "connect");
        setSize(CONNECT_SIZE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { doLeave(); }
        });

        setVisible(true);
    }

    // ====================== MÀN HÌNH KẾT NỐI (Giữ nguyên kích thước) ======================
    private JPanel buildConnectScreen() {
        JPanel screen = new JPanel(new GridBagLayout());
        screen.setBackground(BG_DARK);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(BG_DARK);
        box.setBorder(BorderFactory.createEmptyBorder(0, 36, 0, 36));

        box.add(buildLogo());
        box.add(Box.createVerticalStrut(32));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
            new RoundBorder(BORDER_CLR, 16, 1),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));

        JPanel row1 = new JPanel(new GridLayout(1, 2, 10, 0));
        row1.setBackground(BG_CARD);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row1.add(labeledField("SERVER IP", ipField));
        row1.add(labeledField("PORT", portField));
        card.add(row1);
        card.add(Box.createVerticalStrut(14));

        JPanel nameWrap = labeledField("TÊN CỦA BẠN", nameField);
        nameWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        nameWrap.setAlignmentX(CENTER_ALIGNMENT);
        card.add(nameWrap);
        card.add(Box.createVerticalStrut(10));

        JPanel passWrap = labeledField("MẬT KHẨU PHÒNG", passField);
        passWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        passWrap.setAlignmentX(CENTER_ALIGNMENT);
        card.add(passWrap);
        card.add(Box.createVerticalStrut(20));

        GradBtn btn = new GradBtn("Kết nối →");
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.addActionListener(e -> doConnect());
        card.add(btn);

        ipField.addActionListener(e -> portField.requestFocus());
        portField.addActionListener(e -> nameField.requestFocus());
        nameField.addActionListener(e -> passField.requestFocus());
        passField.addActionListener(e -> doConnect());

        box.add(card);
        box.add(Box.createVerticalStrut(20));
        box.add(buildHintPanel());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        screen.add(box, gbc);
        return screen;
    }

    private JPanel buildLogo() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_DARK);

        JPanel icon = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setPaint(new GradientPaint(0, 0, ACCENT_BLU, getWidth(), getHeight(), ACCENT_PUR));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                String s = "MC";
                g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(56, 56));
        icon.setMaximumSize(new Dimension(56, 56));
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = mk("Chat Client", 22, Font.BOLD, new Color(240, 242, 255));
        title.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sub = mk("Java Socket · TCP/IP", 12, Font.PLAIN, TEXT_MUT);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        p.add(icon);
        p.add(Box.createVerticalStrut(12));
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        return p;
    }

    private JPanel buildHintPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_CARD);
        p.setBorder(new CompoundBorder(
            new RoundBorder(BORDER_CLR, 12, 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel title = mk("Lệnh hỗ trợ", 11, Font.BOLD, TEXT_MUT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        p.add(title);

        String[][] cmds = {
            {"/list", "Danh sách người online"},
            {"/pm <tên> <tin>", "Nhắn tin riêng"},
            {"/quit", "Thoát khỏi phòng"},
        };
        for (String[] c : cmds) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(BG_CARD);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            row.add(mk(c[0], 11, Font.BOLD, ACCENT_BLU), BorderLayout.WEST);
            row.add(mk(c[1], 11, Font.PLAIN, TEXT_MUT), BorderLayout.CENTER);
            p.add(row);
        }
        return p;
    }

    private JPanel labeledField(String labelText, DarkField field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_CARD);
        JLabel lbl = mk(labelText, 10, Font.BOLD, TEXT_MUT);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        p.add(lbl);
        p.add(Box.createVerticalStrut(5));
        p.add(field);
        return p;
    }

    // ====================== MÀN HÌNH CHAT (ĐÃ LÀM TO) ======================
    private JPanel buildChatScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(BG_DARK);
        screen.add(buildTopBar(), BorderLayout.NORTH);

        JPanel centerRow = new JPanel(new BorderLayout());
        centerRow.setBackground(BG_DARK);
        centerRow.add(buildMsgArea(), BorderLayout.CENTER);

        memberSidePanel = buildMemberSidePanel();
        memberSidePanel.setVisible(false);
        centerRow.add(memberSidePanel, BorderLayout.EAST);

        screen.add(centerRow, BorderLayout.CENTER);
        screen.add(buildComposer(), BorderLayout.SOUTH);
        return screen;
    }

    private void switchToChat() {
        cardLayout.show(mainPanel, "chat");
        setSize(CHAT_SIZE);
        setLocationRelativeTo(null);
        revalidate();
        repaint();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_CARD);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_CLR),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setBackground(BG_CARD);

        statusDot = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(Boolean.TRUE.equals(getClientProperty("on")) ? GREEN_DOT : RED_DOT);
                g2.fillOval(0, 3, 9, 9);
                g2.dispose();
            }
        };
        statusDot.setOpaque(false);
        statusDot.setPreferredSize(new Dimension(9, 16));
        statusDot.putClientProperty("on", true);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(BG_CARD);
        lblRoom.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblRoom.setForeground(TEXT_PRI);
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblStatus.setForeground(TEXT_MUT);
        stack.add(lblRoom);
        stack.add(lblStatus);

        left.add(statusDot);
        left.add(stack);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(BG_CARD);

        btnToggleMembers = new JButton() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                boolean active = memberPanelVisible;
                g2.setColor(active ? new Color(40, 50, 90) : new Color(28, 32, 48));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                if (active) {
                    g2.setColor(ACCENT_BLU);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                }
                g2.setColor(active ? ACCENT_BLU : TEXT_MUT);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.fillOval(cx - 4, cy - 9, 8, 8);
                g2.fillArc(cx - 7, cy - 2, 14, 12, 0, 180);
                g2.dispose();
            }
        };
        btnToggleMembers.setPreferredSize(new Dimension(34, 30));
        btnToggleMembers.setOpaque(false);
        btnToggleMembers.setContentAreaFilled(false);
        btnToggleMembers.setBorderPainted(false);
        btnToggleMembers.setFocusPainted(false);
        btnToggleMembers.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleMembers.setToolTipText("Danh sách thành viên");
        btnToggleMembers.addActionListener(e -> toggleMemberPanel());

        JButton leaveBtn = new JButton("Ngắt kết nối");
        leaveBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leaveBtn.setForeground(new Color(239, 68, 68));
        leaveBtn.setBorder(new CompoundBorder(
            new RoundBorder(new Color(60, 30, 30), 8, 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        leaveBtn.setFocusPainted(false);
        leaveBtn.setContentAreaFilled(false);
        leaveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        leaveBtn.addActionListener(e -> doLeave());

        right.add(btnToggleMembers);
        right.add(leaveBtn);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildMemberSidePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_CARD);
        panel.setBorder(new CompoundBorder(
            new MatteBorder(0, 1, 0, 0, BORDER_CLR),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        panel.setPreferredSize(new Dimension(200, 0));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_CLR),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        JLabel title = mk("Thành viên", 12, Font.BOLD, TEXT_PRI);
        lblOnlineCount = mk("0 người trực tuyến", 10, Font.PLAIN, GREEN_DOT);

        JPanel hStack = new JPanel();
        hStack.setLayout(new BoxLayout(hStack, BoxLayout.Y_AXIS));
        hStack.setBackground(BG_CARD);
        hStack.add(title);
        hStack.add(Box.createVerticalStrut(2));
        hStack.add(lblOnlineCount);
        header.add(hStack, BorderLayout.CENTER);
        panel.add(header, BorderLayout.NORTH);

        memberListContainer = new JPanel();
        memberListContainer.setLayout(new BoxLayout(memberListContainer, BoxLayout.Y_AXIS));
        memberListContainer.setBackground(BG_CARD);
        memberListContainer.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JScrollPane memberScroll = new JScrollPane(memberListContainer);
        memberScroll.setBorder(null);
        memberScroll.getViewport().setBackground(BG_CARD);
        memberScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(memberScroll, BorderLayout.CENTER);
        return panel;
    }

    private void toggleMemberPanel() {
        memberPanelVisible = !memberPanelVisible;
        memberSidePanel.setVisible(memberPanelVisible);
        btnToggleMembers.repaint();
        revalidate();
        repaint();
    }

    private void updateMemberList(String systemMsg) {
        int idx = systemMsg.indexOf("Online (");
        if (idx == -1) return;
        int start = idx + "Online (".length();
        int end = systemMsg.indexOf(")", start);
        if (end == -1) return;
        String countStr = systemMsg.substring(start, end).trim();
        int count = Integer.parseInt(countStr);

        List<String> names = new ArrayList<>();
        int colonIdx = systemMsg.indexOf(":", end);
        if (colonIdx != -1 && colonIdx + 2 < systemMsg.length()) {
            String namesPart = systemMsg.substring(colonIdx + 2).trim();
            if (!namesPart.equals("(trống)")) {
                for (String n : namesPart.split(",")) {
                    String nm = n.trim();
                    if (!nm.isEmpty()) names.add(nm);
                }
            }
        }

        final int fCount = count;
        final List<String> fNames = names;
        SwingUtilities.invokeLater(() -> {
            lblOnlineCount.setText(fCount + " người trực tuyến");
            memberListContainer.removeAll();
            for (String name : fNames) {
                memberListContainer.add(buildMemberRow(name));
                memberListContainer.add(Box.createVerticalStrut(2));
            }
            memberListContainer.revalidate();
            memberListContainer.repaint();
        });
    }

    private JPanel buildMemberRow(String name) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        row.setBackground(BG_CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(GREEN_DOT);
                g2.fillOval(0, 4, 8, 8);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(8, 16));

        JLabel nameLbl = mk(name, 12, Font.PLAIN, TEXT_PRI);
        if (name.equals(username)) {
            nameLbl.setForeground(ACCENT_BLU);
            nameLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        }

        row.add(dot);
        row.add(nameLbl);

        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(new Color(25, 28, 44)); }
            public void mouseExited(MouseEvent e) { row.setBackground(BG_CARD); }
        });
        return row;
    }

    private JScrollPane buildMsgArea() {
        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.Y_AXIS));
        msgPanel.setBackground(BG_DARK);
        msgPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 40, 18));

        scroll = new JScrollPane(msgPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        return scroll;
    }

    private JPanel buildComposer() {
        JPanel comp = new JPanel(new BorderLayout(10, 0));
        comp.setBackground(BG_CARD);
        comp.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_CLR),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        msgInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        msgInput.addActionListener(e -> doSend());

        comp.add(msgInput, BorderLayout.CENTER);

        JButton sendBtn = new JButton() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                if (isEnabled())
                    g2.setPaint(new GradientPaint(0, 0, ACCENT_BLU, getWidth(), getHeight(), ACCENT_PUR));
                else
                    g2.setColor(new Color(40, 44, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(isEnabled() ? Color.WHITE : TEXT_MUT);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.drawLine(cx - 6, cy, cx + 5, cy);
                g2.drawLine(cx + 1, cy - 4, cx + 5, cy);
                g2.drawLine(cx + 1, cy + 4, cx + 5, cy);
                g2.dispose();
            }
        };
        sendBtn.setPreferredSize(new Dimension(42, 42));
        sendBtn.setOpaque(false);
        sendBtn.setContentAreaFilled(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> doSend());

        comp.add(sendBtn, BorderLayout.EAST);
        return comp;
    }

    // ====================== ACTIONS ======================
    private void doConnect() {
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();
        String name = nameField.getText().trim();
        String pass = passField.getText().trim();

        if (ip.isEmpty()) ip = "localhost";
        if (name.isEmpty()) name = "Guest_" + (int)(Math.random() * 9000 + 1000);

        int port;
        try {
            port = portStr.isEmpty() ? 1234 : Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            showErr("Port không hợp lệ!");
            portField.requestFocus();
            return;
        }

        username = name;
        final String fIp = ip;
        final int fPort = port;
        final String fName = name;
        final String fPass = pass;

        new Thread(() -> {
            try {
                socket = new Socket(fIp, fPort);
                sockOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

                String firstLine = in.readLine();
                if (firstLine != null && firstLine.equals("SYSTEM:PASSWORD_REQUIRED")) {
                    sockOut.println(fPass);
                    firstLine = in.readLine();
                    if (firstLine != null && firstLine.startsWith("SYSTEM:Sai mật khẩu")) {
                        SwingUtilities.invokeLater(() -> showErr("Sai mật khẩu phòng!"));
                        socket.close();
                        return;
                    }
                }

                sockOut.println(fName);
                connected = true;
                wasKicked = false;

                SwingUtilities.invokeLater(() -> {
                    lblRoom.setText("Phòng chat");
                    lblStatus.setText(fIp + ":" + fPort + " · " + fName);
                    setDotOn(true);
                    switchToChat();                    // ← Chuyển màn và thay đổi kích thước
                    addSysMsg("Đã kết nối tới " + fIp + ":" + fPort);
                });

                if (firstLine != null) {
                    final String fl = firstLine;
                    SwingUtilities.invokeLater(() -> receiveMsg(fl));
                }

                String msg;
                while ((msg = in.readLine()) != null) {
                    final String line = msg;
                    SwingUtilities.invokeLater(() -> receiveMsg(line));
                }
            } catch (IOException ex) {
                if (connected)
                    SwingUtilities.invokeLater(() -> showErr("Lỗi kết nối:\n" + ex.getMessage()));
            } finally {
                connected = false;
                final boolean kicked = wasKicked;
                SwingUtilities.invokeLater(() -> {
                    setDotOn(false);
                    if (kicked) {
                        returnToLogin("Bạn đã bị Admin kick khỏi phòng.");
                    }
                });
            }
        }, "recv-thread").start();
    }

    private void doSend() {
        if (sockOut == null || !connected) return;
        String text = msgInput.getText().trim();
        if (text.isEmpty()) return;
        sockOut.println(text);
        msgInput.setText("");
    }

    private void doLeave() {
        connected = false;
        wasKicked = false;
        try { if (sockOut != null) sockOut.println("/quit"); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        sockOut = null;

        SwingUtilities.invokeLater(() -> {
            resetChatScreen();
            setSize(CONNECT_SIZE);
            setLocationRelativeTo(null);
            cardLayout.show(mainPanel, "connect");
        });
    }

    private void returnToLogin(String reason) {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        sockOut = null;
        resetChatScreen();

        SwingUtilities.invokeLater(() -> {
            setSize(CONNECT_SIZE);
            setLocationRelativeTo(null);
            cardLayout.show(mainPanel, "connect");
            JOptionPane.showMessageDialog(this,
                "<html><b>Bạn đã bị ngắt kết nối</b><br><br>" + reason + "</html>",
                "Thông báo từ Server", JOptionPane.WARNING_MESSAGE);
        });
    }

    private void resetChatScreen() {
        msgPanel.removeAll();
        msgPanel.revalidate();
        msgPanel.repaint();
        setDotOn(false);
        memberPanelVisible = false;
        if (memberSidePanel != null) memberSidePanel.setVisible(false);
        if (memberListContainer != null) memberListContainer.removeAll();
        if (lblOnlineCount != null) lblOnlineCount.setText("0 người trực tuyến");
    }

    // ====================== HIỂN THỊ TIN NHẮN ======================
    private void receiveMsg(String raw) {
        if (raw.startsWith("ROOMNAME:")) {
            lblRoom.setText(raw.substring(9));
            return;
        }
        if (raw.startsWith("SYSTEM:")) {
            String body = raw.substring(7);
            if (body.contains("bị kick") || body.toLowerCase().contains("kicked")) {
                wasKicked = true;
                connected = false;
                returnToLogin(body);
                return;
            }
            if (body.contains("Online (")) updateMemberList(body);
            addSysMsg(body);
            return;
        }
        if (raw.contains("[PM từ ") || raw.contains("[PM gửi ")) {
            addPmBubble(raw);
            return;
        }

        try {
            int closeBracket = raw.indexOf("] ");
            if (closeBracket == -1) { addSysMsg(raw); return; }
            String remain = raw.substring(closeBracket + 2);
            int sep = remain.indexOf(": ");
            if (sep == -1) { addSysMsg(raw); return; }
            String sender = remain.substring(0, sep);
            String msg = remain.substring(sep + 2);

            if (sender.equals(username)) addOwnBubble(msg);
            else addOtherBubble(sender, msg);
        } catch (Exception e) {
            addSysMsg(raw);
        }
    }

    private void addOwnBubble(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        row.setBackground(BG_DARK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(BG_DARK);
        col.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel time = mk(now(), 10, Font.PLAIN, TEXT_MUT);
        time.setAlignmentX(RIGHT_ALIGNMENT);

        Bubble b = new Bubble(text, true);
        b.setAlignmentX(RIGHT_ALIGNMENT);

        col.add(time);
        col.add(b);
        row.add(col);
        appendRow(row);
    }

    private void addOtherBubble(String sender, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        row.setBackground(BG_DARK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(new Avatar(sender));

        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(BG_DARK);

        JLabel meta = mk(sender + " • " + now(), 10, Font.PLAIN, TEXT_MUT);
        meta.setAlignmentX(LEFT_ALIGNMENT);

        Bubble b = new Bubble(text, false);
        b.setAlignmentX(LEFT_ALIGNMENT);

        col.add(meta);
        col.add(b);
        row.add(col);
        appendRow(row);
    }

    private void addPmBubble(String raw) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        row.setBackground(BG_DARK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pmCard = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(new Color(30, 20, 55));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(80, 50, 130));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pmCard.setLayout(new BoxLayout(pmCard, BoxLayout.Y_AXIS));
        pmCard.setOpaque(false);
        pmCard.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JLabel tag = mk("Tin nhắn riêng", 10, Font.BOLD, new Color(139, 92, 246));
        JLabel txt = mk(raw, 12, Font.PLAIN, TEXT_PRI);
        tag.setAlignmentX(CENTER_ALIGNMENT);
        txt.setAlignmentX(CENTER_ALIGNMENT);

        pmCard.add(tag);
        pmCard.add(Box.createVerticalStrut(3));
        pmCard.add(txt);

        row.add(pmCard);
        appendRow(row);
    }

    private void addSysMsg(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        row.setBackground(BG_DARK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pill = new JLabel(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(BORDER_CLR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("SansSerif", Font.PLAIN, 11));
        pill.setForeground(TEXT_MUT);
        pill.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        pill.setOpaque(false);

        row.add(pill);
        appendRow(row);
    }

    private void appendRow(JPanel row) {
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        msgPanel.add(row);
        msgPanel.add(Box.createVerticalStrut(9));   // Khoảng cách giữa tin nhắn
        msgPanel.revalidate();
        msgPanel.repaint();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            if (scroll != null) {
                JScrollBar sb = scroll.getVerticalScrollBar();
                sb.setValue(sb.getMaximum());
            }
        });
    }

    // ====================== INNER CLASSES ======================
    static class Bubble extends JPanel {
        private final String text;
        private final boolean own;
        private static final int PH = 14, PV = 9, R = 16;

        Bubble(String text, boolean own) {
            this.text = text; this.own = own;
            setOpaque(false);
            setFont(new Font("SansSerif", Font.PLAIN, 13));
        }

        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int maxW = 280, lineW = 0, maxLW = 0, lines = 1;
            for (String w : text.split("(?<=\\s)|(?=\\s)")) {
                int ww = fm.stringWidth(w);
                if (lineW + ww > maxW - PH * 2 && lineW > 0) {
                    maxLW = Math.max(maxLW, lineW); lines++; lineW = ww;
                } else lineW += ww;
            }
            maxLW = Math.max(maxLW, lineW);
            return new Dimension(Math.min(maxLW + PH * 2, maxW), lines * fm.getHeight() + PV * 2);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(own ? BUBBLE_OWN : BUBBLE_OTH);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), R, R);
            if (!own) {
                g2.setColor(BORDER_CLR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, R, R);
            }
            g2.setColor(own ? Color.WHITE : TEXT_PRI);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = PH, y = PV + fm.getAscent(), maxW = getWidth() - PH * 2;
            StringBuilder line = new StringBuilder();
            for (String w : text.split("(?<=\\s)|(?=\\s)")) {
                String test = line + w;
                if (fm.stringWidth(test) > maxW && line.length() > 0) {
                    g2.drawString(line.toString().trim(), x, y);
                    y += fm.getHeight();
                    line = new StringBuilder(w);
                } else line.append(w);
            }
            if (line.length() > 0) g2.drawString(line.toString().trim(), x, y);
            g2.dispose();
        }
    }

    static class Avatar extends JPanel {
        private static final Color[] BGS = {new Color(42,58,122), new Color(58,31,90), new Color(26,58,42),
                new Color(58,42,26), new Color(58,26,42), new Color(26,42,58)};
        private static final Color[] FGS = {new Color(79,110,247), new Color(139,92,246), new Color(34,197,94),
                new Color(245,158,11), new Color(239,68,68), new Color(20,184,166)};

        private final String init;
        private final Color bg, fg;

        Avatar(String name) {
            int h = Math.abs(name.hashCode()) % BGS.length;
            bg = BGS[h]; fg = FGS[h];
            String[] p = name.split("\\s+");
            if (p.length >= 2) init = ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase();
            else if (name.length() >= 2) init = name.substring(0, 2).toUpperCase();
            else init = name.toUpperCase();
            setPreferredSize(new Dimension(32, 32));
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(bg);
            g2.fillOval(0, 0, 32, 32);
            g2.setColor(fg);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(init, (32 - fm.stringWidth(init)) / 2,
                    (32 + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    static class DarkField extends JTextField {
        private final String ph;
        DarkField(String ph) {
            this.ph = ph;
            setOpaque(false);
            setForeground(TEXT_PRI);
            setCaretColor(ACCENT_BLU);
            setFont(new Font("SansSerif", Font.PLAIN, 13));
            applyBorder(BORDER_CLR);
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { applyBorder(ACCENT_BLU); repaint(); }
                public void focusLost(FocusEvent e) { applyBorder(BORDER_CLR); repaint(); }
            });
        }
        private void applyBorder(Color c) {
            setBorder(new CompoundBorder(new RoundBorder(c, 10, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(BG_DARK);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_MUT);
                g2.setFont(getFont());
                Insets ins = getInsets();
                g2.drawString(ph, ins.left, ins.top + g2.getFontMetrics().getAscent());
                g2.dispose();
            }
        }
    }

    static class RoundBorder extends AbstractBorder {
        private final Color c; private final int r, t;
        RoundBorder(Color c, int r, int t) { this.c = c; this.r = r; this.t = t; }
        public void paintBorder(Component comp, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = aa(g);
            g2.setColor(c);
            g2.setStroke(new BasicStroke(t));
            g2.drawRoundRect(x, y, w-1, h-1, r, r);
            g2.dispose();
        }
        public Insets getBorderInsets(Component c) { return new Insets(t, t, t, t); }
    }

    static class GradBtn extends JButton {
        GradBtn(String text) {
            super(text);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setPaint(new GradientPaint(0, 0, ACCENT_BLU, getWidth(), getHeight(), ACCENT_PUR));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ====================== UTILS ======================
    private static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    private static JLabel mk(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private static String now() {
        return new SimpleDateFormat("HH:mm").format(new Date());
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private void setDotOn(boolean on) {
        if (statusDot != null) {
            statusDot.putClientProperty("on", on);
            statusDot.repaint();
        }
    }
}
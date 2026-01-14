package edu.univ.erp.ui.auth;

import edu.univ.erp.auth.AuthService;
import edu.univ.erp.data.NotificationDAO;
import edu.univ.erp.ui.admin.AdminDashboard;
import edu.univ.erp.ui.instructor.InstructorDashboard;
import edu.univ.erp.ui.student.StudentDashboard;
import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final AuthService authService = new AuthService();

    private final Color CARD_BG = new Color(255, 255, 255, 210);
    private final Color ACTION_BG = Color.decode("#DA7385");

    private int failedAttempts = 0;
    private final int MAX_ATTEMPTS = 5;
    private final int LOCKOUT_SECONDS = 30;
    private long lockoutEndMillis = 0L;
    private Timer lockoutTimer;

    public LoginFrame() {

        setTitle("IIITD ERP - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 550);
        setLocationRelativeTo(null);
        setResizable(false);

        ImageIcon bgIcon = null;
        try {
            bgIcon = new ImageIcon(
                    Objects.requireNonNull(
                            getClass().getResource("/iiit_picture.jpeg"))
            );
        } catch (Exception ex) {}

        JLabel background;
        if (bgIcon != null) {
            Image bg = bgIcon.getImage().getScaledInstance(900, 550, Image.SCALE_SMOOTH);
            background = new JLabel(new ImageIcon(bg));
        } else {
            background = new JLabel();
            background.setOpaque(true);
            background.setBackground(Color.LIGHT_GRAY);
        }
        background.setLayout(new GridBagLayout());
        add(background);

        JPanel card = new JPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(380, 360));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,180), 2, true));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("IIIT ERP LOGIN", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        card.add(title, gbc);

        gbc.gridwidth = 1;

        JLabel userLabel = new JLabel("Username:");
        userLabel.setHorizontalAlignment(SwingConstants.LEFT);
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 12, 2, 12);
        card.add(userLabel, gbc);

        styleField(usernameField);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 12, 10, 12);
        card.add(usernameField, gbc);

        gbc.gridwidth = 1;

        JLabel passLabel = new JLabel("Password:");
        passLabel.setHorizontalAlignment(SwingConstants.LEFT);
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.insets = new Insets(5, 12, 2, 12);
        card.add(passLabel, gbc);

        styleField(passwordField);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 12, 15, 12);
        card.add(passwordField, gbc);

        gbc.gridwidth = 1;

        JButton loginBtn = new JButton("LOGIN");
        loginBtn.setBackground(ACTION_BG);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        loginBtn.setPreferredSize(new Dimension(90, 32));
        loginBtn.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        loginBtn.addActionListener(e -> handleLogin(loginBtn));

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(loginBtn, gbc);

        background.add(card);

        lockoutTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.currentTimeMillis();
                if (now >= lockoutEndMillis) {
                    unlockUI(loginBtn);
                }
            }
        });
    }

    private void styleField(JTextField field) {
        field.setPreferredSize(new Dimension(300, 35));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    private void handleLogin(JButton loginBtn) {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());

        AuthService.AuthResult res = authService.login(u, p);

        if (!res.success) {
            failedAttempts++;
            int remaining = Math.max(0, MAX_ATTEMPTS - failedAttempts);
            if (failedAttempts >= MAX_ATTEMPTS) {
                startLockout(loginBtn);
                JOptionPane.showMessageDialog(this, "Too many failed attempts, login temporarily locked for " + LOCKOUT_SECONDS + " seconds.");
            } else {
                if (remaining == 1) {
                    JOptionPane.showMessageDialog(this, "Login failed. One more failed attempt will temporarily lock login.");
                } else {
                    JOptionPane.showMessageDialog(this, "Login failed. Attempts left: " + remaining);
                }
            }
            return;
        }

        resetLockoutState();
        JOptionPane.showMessageDialog(this, "Welcome, " + u);
        this.dispose();

        switch (res.role.toUpperCase()) {
            case "ADMIN" -> new AdminDashboard(res.userId).setVisible(true);
            case "INSTRUCTOR" -> new InstructorDashboard(res.userId).setVisible(true);
            case "STUDENT" -> {
                new NotificationDAO().add(res.userId, "Logged in");
                new StudentDashboard(res.userId).setVisible(true);
            }
            default -> JOptionPane.showMessageDialog(null, "Unknown role: " + res.role);
        }
    }

    private void startLockout(JButton loginBtn) {
        lockoutEndMillis = System.currentTimeMillis() + LOCKOUT_SECONDS * 1000L;
        usernameField.setEnabled(false);
        passwordField.setEnabled(false);
        loginBtn.setEnabled(false);
        loginBtn.setToolTipText("Locked — try again in " + LOCKOUT_SECONDS + "s");
        if (!lockoutTimer.isRunning()) lockoutTimer.start();
    }

    private void unlockUI(JButton loginBtn) {
        usernameField.setEnabled(true);
        passwordField.setEnabled(true);
        loginBtn.setEnabled(true);
        loginBtn.setToolTipText(null);
        resetLockoutState();
        if (lockoutTimer.isRunning()) lockoutTimer.stop();
        JOptionPane.showMessageDialog(this, "Lockout expired — you may try logging in again.");
    }

    private void resetLockoutState() {
        failedAttempts = 0;
        lockoutEndMillis = 0L;
        if (lockoutTimer.isRunning()) lockoutTimer.stop();
    }

}
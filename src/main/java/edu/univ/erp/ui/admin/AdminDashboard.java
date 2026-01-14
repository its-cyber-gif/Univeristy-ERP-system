package edu.univ.erp.ui.admin;
import edu.univ.erp.auth.PasswordHash;
import edu.univ.erp.config.DBConfig;
import edu.univ.erp.access.AccessControl;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.*;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.TimePicker;

public class AdminDashboard extends JFrame {
    private final int adminId;
    private String adminUsername = "Admin";

    private final Color SIDEBAR_BG = Color.decode("#D65D72");
    private final Color SIDEBAR_HOVER = Color.decode("#EEA8B4");
    private final Color CARD_DEFAULT = Color.decode("#EEA8B4");
    private final Color CARD_HOVER = Color.decode("#DA7385");
    private final Color ACTION_BG = Color.decode("#DA7385");
    private final Color WHITE = Color.WHITE;

    private CardLayout cards = new CardLayout();
    private JPanel centerPanel = new JPanel(cards);

    private final CourseDAO courseDAO = new CourseDAO();
    private final SectionDAO sectionDAO = new SectionDAO();

    private JTable activeTable;
    private TableRowSorter<DefaultTableModel> activeSorter;
    private JTextField searchField;

    public AdminDashboard(int adminId) {
        this.adminId = adminId;
        setTitle("University ERP");
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        fetchAdminUsername();

        setLayout(new BorderLayout());
        add(buildLeftMediumSidebar(), BorderLayout.WEST);
        add(buildTopBar(), BorderLayout.NORTH);

        centerPanel.add(buildHomePanel(), "HOME");
        centerPanel.add(buildStudentsPanel(), "STUDENTS");
        centerPanel.add(buildInstructorsPanel(), "INSTRUCTORS");
        centerPanel.add(buildCoursesPanel(), "COURSES");
        centerPanel.add(buildSectionsPanel(), "SECTIONS");
        centerPanel.add(buildAdminsPanel(), "ADMINS");
        centerPanel.add(buildSettingsPanel(), "SETTINGS");
        add(centerPanel, BorderLayout.CENTER);
        cards.show(centerPanel, "HOME");
    }

    private void fetchAdminUsername() {
        try (Connection c = DBConfig.getAuthDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT username FROM users_auth WHERE user_id=?")) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) adminUsername = rs.getString(1);
            }
        } catch (Exception ignored) { }
    }

    private JPanel buildLeftMediumSidebar() {
        JPanel side = new JPanel();
        side.setBackground(SIDEBAR_BG);
        side.setPreferredSize(new Dimension(180, 0));
        side.setLayout(new BorderLayout());

        JPanel items = new JPanel();
        items.setOpaque(false);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBorder(new EmptyBorder(12, 10, 12, 10));

        items.add(makeSideButton("", "Home", e -> { cards.show(centerPanel, "HOME"); }));
        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(makeSideButton("", "Students", e -> {
            cards.show(centerPanel, "STUDENTS");
            SwingUtilities.invokeLater(() -> activateCurrentTab());
        }));

        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(makeSideButton("", "Instructors", e -> {
            cards.show(centerPanel, "INSTRUCTORS");
            SwingUtilities.invokeLater(() -> activateCurrentTab());
        }));

        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(makeSideButton("", "Courses", e -> {
            cards.show(centerPanel, "COURSES");
            SwingUtilities.invokeLater(() -> activateCurrentTab());
        }));

        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(makeSideButton("", "Sections", e -> {
            cards.show(centerPanel, "SECTIONS");
            SwingUtilities.invokeLater(() -> activateCurrentTab());
        }));

        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(makeSideButton("", "Admins", e -> {
            cards.show(centerPanel, "ADMINS");
            SwingUtilities.invokeLater(() -> activateCurrentTab());
        }));
        items.add(makeSideButton("", "Settings", e -> {
            cards.show(centerPanel, "SETTINGS");
        }));

        items.add(Box.createRigidArea(new Dimension(0, 8)));


        side.add(items, BorderLayout.NORTH);

        JPanel prof = new JPanel(new BorderLayout());
        prof.setOpaque(false);
        prof.setBorder(new EmptyBorder(12, 10, 12, 10));

        JLabel name = new JLabel("<html><b>" + adminUsername + "</b><br/><small>Administrator</small></html>");
        name.setForeground(WHITE);

        prof.add(name, BorderLayout.CENTER);
        side.add(prof, BorderLayout.SOUTH);

        return side;
    }

    private JPanel buildAdminsPanel() {

        String[] cols = {"User ID", "Username", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c != 0; }
            @Override public Class<?> getColumnClass(int c) { return (c == 0) ? Integer.class : String.class; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(26);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        refreshAdmins(model);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JButton add = styledActionButton("+ Add");
        JButton save = styledActionButton("Save");
        JButton del = styledActionButton("Delete");

        actions.add(add);
        actions.add(del);
        actions.add(save);

        add.addActionListener(e -> openAddAdminDialog(model));
        save.addActionListener(e -> saveAllAdmins(model));
        del.addActionListener(e -> askDeleteAdmin(model));

        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        activateTable(table, sorter);

        return panel;
    }
    private void refreshAdmins(DefaultTableModel model) {
        model.setRowCount(0);

        try (Connection con = DBConfig.getAuthDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT user_id, username, status FROM users_auth WHERE role = 'ADMIN' ORDER BY user_id"
             );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("status")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void openAddAdminDialog(DefaultTableModel model) {
        JTextField username = new JTextField();
        JPasswordField pwd = new JPasswordField();

        Object[] msg = {
                "Username:", username,
                "Password:", pwd
        };

        if (JOptionPane.showConfirmDialog(this, msg, "Add Admin",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try (Connection con = DBConfig.getAuthDataSource().getConnection()) {

            String hash = PasswordHash.hash(new String(pwd.getPassword()));

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users_auth (username, role, password_hash, status) VALUES (?,?,?,?)"
            );
            ps.setString(1, username.getText());
            ps.setString(2, "ADMIN");
            ps.setString(3, hash);
            ps.setString(4, "ACTIVE");
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Admin added!");
            refreshAdmins(model);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }
    private void saveAllAdmins(DefaultTableModel m) {
        try (Connection con = DBConfig.getAuthDataSource().getConnection()) {
            for (int r = 0; r < m.getRowCount(); r++) {
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE users_auth SET username=?, status=? WHERE user_id=? AND role='ADMIN'"
                );
                ps.setString(1, m.getValueAt(r, 1).toString());
                ps.setString(2, m.getValueAt(r, 2).toString());
                ps.setInt(3, (int) m.getValueAt(r, 0));
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }
        JOptionPane.showMessageDialog(this, "All admins saved!");
    }
    private void askDeleteAdmin(DefaultTableModel model) {
        String s = JOptionPane.showInputDialog(this, "Enter Admin USER_ID to delete:");
        if (s == null || s.isBlank()) return;

        try {
            int uid = Integer.parseInt(s.trim());

            int c = JOptionPane.showConfirmDialog(this,
                    "Delete admin " + uid + "?",
                    "Confirm",
                    JOptionPane.OK_CANCEL_OPTION);

            if (c != JOptionPane.OK_OPTION) return;

            deleteAdmin(uid);
            refreshAdmins(model);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid ID");
        }
    }

    private void deleteAdmin(int uid) {
        try (Connection con = DBConfig.getAuthDataSource().getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM users_auth WHERE user_id=? AND role='ADMIN'"
            );
            ps.setInt(1, uid);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Admin deleted!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private JButton makeSideButton(String icon, String label, ActionListener act) {
        JButton b = new JButton(icon + "   " + label);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setBackground(SIDEBAR_BG);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        b.addActionListener(act);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(SIDEBAR_HOVER); b.setForeground(Color.BLACK); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(SIDEBAR_BG); b.setForeground(WHITE); }
            @Override public void mousePressed(MouseEvent e) { b.setBackground(SIDEBAR_HOVER.darker()); }
            @Override public void mouseReleased(MouseEvent e) { b.setBackground(SIDEBAR_HOVER); }
        });
        return b;
    }

    private MouseAdapter sideHoverAdapter(JComponent c) {
        return new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { c.setBackground(SIDEBAR_HOVER); c.setForeground(Color.BLACK); }
            @Override public void mouseExited(MouseEvent e) { c.setBackground(SIDEBAR_BG); c.setForeground(WHITE); }
        };
    }

    private void showProfilePopup(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem change = new JMenuItem("Change Password");
        JMenuItem logout = new JMenuItem("Logout");
        change.addActionListener(e -> changeAdminPassword());
        logout.addActionListener(e -> {
            dispose();
            JOptionPane.showMessageDialog(null, "Logged out.");
            SwingUtilities.invokeLater(() -> new edu.univ.erp.ui.auth.LoginFrame().setVisible(true));
        });
        menu.add(change);
        menu.add(logout);
        menu.show(invoker, 0, invoker.getHeight());
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("Admin Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        top.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(280, 28));
        searchField.setToolTipText("Search active table...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                applySearch(searchField.getText());
            }
        });
        right.add(searchField);

        JButton profileBtn = new JButton("👤");
        profileBtn.setPreferredSize(new Dimension(34, 34));
        profileBtn.setBackground(Color.WHITE);
        profileBtn.setFocusPainted(false);
        profileBtn.setBorder(BorderFactory.createEmptyBorder());
        profileBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileBtn.setContentAreaFilled(false);
        profileBtn.setOpaque(true);
        profileBtn.setBackground(new Color(230, 230, 230));
        profileBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));

        profileBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                profileBtn.setBackground(new Color(210, 210, 210));
            }
            @Override public void mouseExited(MouseEvent e) {
                profileBtn.setBackground(new Color(230, 230, 230));
            }
        });

        profileBtn.addActionListener(e -> showProfilePopup(profileBtn));

        right.add(profileBtn);
        top.add(right, BorderLayout.EAST);

        return top;
    }


    private void applySearch(String txt) {
        if (activeSorter == null) return;
        if (txt == null || txt.trim().isEmpty()) activeSorter.setRowFilter(null);
        else activeSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(txt)));
    }

    private void activateTable(JTable table, TableRowSorter<DefaultTableModel> sorter) {
        this.activeTable = table;
        this.activeSorter = sorter;
        if (searchField != null) applySearch(searchField.getText());
    }


    private JPanel buildHomePanel() {
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT, 28, 28));
        p.setBackground(Color.WHITE);

        JPanel s = makeBigCard("Total Students", getCount("students"));
        s.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cards.show(centerPanel, "STUDENTS"); }
        });

        JPanel i = makeBigCard("Total Instructors", getCount("instructors"));
        i.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cards.show(centerPanel, "INSTRUCTORS"); }
        });

        JPanel c = makeBigCard("Total Courses", getCount("courses"));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cards.show(centerPanel, "COURSES"); }
        });

        p.add(s); p.add(i); p.add(c);
        return p;
    }

    private JPanel makeBigCard(String title, int value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(260, 140));
        card.setBackground(CARD_DEFAULT);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));
        JLabel v = new JLabel(String.valueOf(value), SwingConstants.CENTER);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 32f));

        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.setBackground(CARD_HOVER);
                card.revalidate();
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBackground(CARD_DEFAULT);
                card.revalidate();
            }
        });

        return card;
    }

    private int getCount(String table) {
        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }


    private JPanel buildStudentsPanel() {
        String[] cols = {"User ID", "Username", "Roll No", "Program", "Year", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c != 0; }
            @Override public Class<?> getColumnClass(int c) { return (c==0||c==4)? Integer.class : String.class; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(26);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        refreshStudents(model);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(12,12,12,12));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton add = styledActionButton("+ Add");
        JButton del = styledActionButton("Delete");
        JButton save = styledActionButton("Save");
        actions.add(add); actions.add(del); actions.add(save);

        add.addActionListener(e -> openAddStudentDialog(model));
        del.addActionListener(e -> askDeleteStudent(model));
        save.addActionListener(e -> saveAllStudents(model));

        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        activateTable(table, sorter);

        return panel;
    }

    private void refreshStudents(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection erp = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = erp.prepareStatement("SELECT user_id, roll_no, program, year FROM students ORDER BY user_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int uid = rs.getInt("user_id");
                String roll = rs.getString("roll_no");
                String program = rs.getString("program");
                int year = rs.getInt("year");
                String username = "", status = "";
                try (Connection auth = DBConfig.getAuthDataSource().getConnection();
                     PreparedStatement ps2 = auth.prepareStatement("SELECT username, status FROM users_auth WHERE user_id=?")) {
                    ps2.setInt(1, uid);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) { username = rs2.getString(1); status = rs2.getString(2); }
                    }
                }
                model.addRow(new Object[]{uid, username, roll, program, year, status});
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void askDeleteStudent(DefaultTableModel model) {
        String s = JOptionPane.showInputDialog(this, "Enter USER_ID to delete:");
        if (s == null || s.isBlank()) return;
        try {
            int uid = Integer.parseInt(s.trim());
            int c = JOptionPane.showConfirmDialog(this, "Delete student " + uid + "?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
            if (c != JOptionPane.OK_OPTION) return;
            deleteStudent(uid);
            refreshStudents(model);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Invalid id"); }
    }

    private void deleteStudent(int uid) {
        try (Connection erp = DBConfig.getErpDataSource().getConnection()) {
            erp.prepareStatement("DELETE FROM enrollments WHERE student_id = " + uid).executeUpdate();
            erp.prepareStatement("DELETE FROM students WHERE user_id = " + uid).executeUpdate();
        } catch (Exception ignored) {}
        try (Connection auth = DBConfig.getAuthDataSource().getConnection()) {
            auth.prepareStatement("DELETE FROM users_auth WHERE user_id = " + uid).executeUpdate();
        } catch (Exception ignored) {}
        JOptionPane.showMessageDialog(this, "Student deleted");
    }

    private void saveAllStudents(DefaultTableModel m) {
        for (int r = 0; r < m.getRowCount(); r++) {
            try (Connection erp = DBConfig.getErpDataSource().getConnection();
                 Connection auth = DBConfig.getAuthDataSource().getConnection()) {
                int uid = (int) m.getValueAt(r, 0);
                PreparedStatement ps = erp.prepareStatement("UPDATE students SET roll_no=?, program=?, year=? WHERE user_id=?");
                ps.setString(1, m.getValueAt(r, 2).toString());
                ps.setString(2, m.getValueAt(r, 3).toString());
                ps.setInt(3, Integer.parseInt(m.getValueAt(r, 4).toString()));
                ps.setInt(4, uid);
                ps.executeUpdate();
                PreparedStatement ps2 = auth.prepareStatement("UPDATE users_auth SET username=?, status=? WHERE user_id=?");
                ps2.setString(1, m.getValueAt(r, 1).toString());
                ps2.setString(2, m.getValueAt(r, 5).toString());
                ps2.setInt(3, uid);
                ps2.executeUpdate();
            } catch (Exception ignored) {}
        }
        JOptionPane.showMessageDialog(this, "All students saved");
    }

    private void openAddStudentDialog(DefaultTableModel model) {
        JTextField username = new JTextField(), roll = new JTextField(), prog = new JTextField(), year = new JTextField();
        JPasswordField pwd = new JPasswordField();
        Object[] msg = {"Username:", username, "Roll No:", roll, "Program:", prog, "Year:", year, "Password:", pwd};
        if (JOptionPane.showConfirmDialog(this, msg, "Add Student", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try (Connection auth = DBConfig.getAuthDataSource().getConnection();
             Connection erp = DBConfig.getErpDataSource().getConnection()) {
            String hash = PasswordHash.hash(new String(pwd.getPassword()));
            PreparedStatement ps = auth.prepareStatement("INSERT INTO users_auth(username, role, password_hash, status) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username.getText()); ps.setString(2, "STUDENT"); ps.setString(3, hash); ps.setString(4, "ACTIVE");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    PreparedStatement ps2 = erp.prepareStatement("INSERT INTO students(user_id, roll_no, program, year) VALUES(?,?,?,?)");
                    ps2.setInt(1, id); ps2.setString(2, roll.getText()); ps2.setString(3, prog.getText()); ps2.setInt(4, Integer.parseInt(year.getText()));
                    ps2.executeUpdate();
                }
            }
            refreshStudents(model);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }


    private JPanel buildInstructorsPanel() {
        String[] cols = {"User ID", "Username", "Department", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c != 0; }
            @Override public Class<?> getColumnClass(int c) { return (c==0||c==4)? Integer.class : String.class; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(26);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        refreshInstructors(model);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(12,12,12,12));


        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton add = styledActionButton("+ Add");
        JButton del = styledActionButton("Delete");
        JButton save = styledActionButton("Save");
        actions.add(add); actions.add(del); actions.add(save);

        add.addActionListener(e -> openAddInstructorDialog(model));
        del.addActionListener(e -> askDeleteInstructor(model));
        save.addActionListener(e -> saveAllInstructors(model));

        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        activateTable(table, sorter);

        return panel;
    }
    private void saveInstructorRow(DefaultTableModel m, int row) {
        try {
            int uid = (int) m.getValueAt(row, 0);
            String username = m.getValueAt(row, 1).toString();
            String dept = m.getValueAt(row, 2).toString();
            String status = m.getValueAt(row, 3).toString();

            try (Connection erp = DBConfig.getErpDataSource().getConnection()) {
                PreparedStatement ps = erp.prepareStatement(
                        "UPDATE instructors SET department = ? WHERE user_id = ?");
                ps.setString(1, dept);
                ps.setInt(2, uid);
                ps.executeUpdate();
            }

            try (Connection auth = DBConfig.getAuthDataSource().getConnection()) {
                PreparedStatement ps2 = auth.prepareStatement(
                        "UPDATE users_auth SET username = ?, status = ? WHERE user_id = ?");
                ps2.setString(1, username);
                ps2.setString(2, status);
                ps2.setInt(3, uid);
                ps2.executeUpdate();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void saveAllInstructors(DefaultTableModel m) {
        for (int row = 0; row < m.getRowCount(); row++) {
            saveInstructorRow(m, row);
        }
        JOptionPane.showMessageDialog(this, "ALL INSTRUCTORS SAVED");
    }
    private void refreshInstructors(DefaultTableModel model) {
        model.setRowCount(0);

        try (Connection erp = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = erp.prepareStatement(
                     "SELECT user_id, department FROM instructors ORDER BY user_id");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                int uid = rs.getInt("user_id");
                String dept = rs.getString("department");

                String username = "";
                String status = "";

                try (Connection auth = DBConfig.getAuthDataSource().getConnection();
                     PreparedStatement ps2 = auth.prepareStatement(
                             "SELECT username, status FROM users_auth WHERE user_id = ?")) {

                    ps2.setInt(1, uid);
                    ResultSet rs2 = ps2.executeQuery();

                    if (rs2.next()) {
                        username = rs2.getString("username");
                        status = rs2.getString("status");
                    }
                }

                model.addRow(new Object[]{
                        uid, username, dept, status
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void askDeleteInstructor(DefaultTableModel model) {
        String uidStr = JOptionPane.showInputDialog(this,
                "Enter USER_ID of instructor to delete:");

        if (uidStr == null || uidStr.isBlank()) return;

        try {
            int uid = Integer.parseInt(uidStr.trim());
            deleteInstructor(uid);
            refreshInstructors(model);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid User ID");
        }
    }

    private void deleteInstructor(int uid) {
        try (Connection erp = DBConfig.getErpDataSource().getConnection()) {

            erp.prepareStatement(
                    "UPDATE sections SET instructor_id = NULL WHERE instructor_id = " + uid
            ).executeUpdate();

            erp.prepareStatement(
                    "DELETE FROM instructors WHERE user_id = " + uid
            ).executeUpdate();

            try (Connection auth = DBConfig.getAuthDataSource().getConnection()) {
                auth.prepareStatement(
                        "DELETE FROM users_auth WHERE user_id = " + uid
                ).executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Instructor deleted successfully.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void openAddInstructorDialog(DefaultTableModel model) {
        JTextField username = new JTextField();
        JTextField dept = new JTextField();
        JPasswordField pwd = new JPasswordField();

        Object[] msg = {
                "Username:", username,
                "Department:", dept,
                "Password:", pwd
        };

        int opt = JOptionPane.showConfirmDialog(this, msg,
                "Add New Instructor", JOptionPane.OK_CANCEL_OPTION);

        if (opt != JOptionPane.OK_OPTION) return;

        try (Connection auth = DBConfig.getAuthDataSource().getConnection();
             Connection erp = DBConfig.getErpDataSource().getConnection()) {

            String hash = PasswordHash.hash(new String(pwd.getPassword()));

            int userId;
            try (PreparedStatement ps = auth.prepareStatement(
                    "INSERT INTO users_auth (username, role, password_hash, status) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username.getText());
                ps.setString(2, "INSTRUCTOR");
                ps.setString(3, hash);
                ps.setString(4, "ACTIVE");
                ps.executeUpdate();
                var rs = ps.getGeneratedKeys();
                rs.next();
                userId = rs.getInt(1);
            }

            try (PreparedStatement ps2 = erp.prepareStatement(
                    "INSERT INTO instructors (user_id, department) VALUES (?,?)")) {
                ps2.setInt(1, userId);
                ps2.setString(2, dept.getText());
                ps2.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Instructor created!");
            refreshInstructors(model);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }


    private JPanel buildCoursesPanel() {
        String[] cols = {"Course ID", "Code", "Title", "Credits"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c != 0; }
            @Override public Class<?> getColumnClass(int c) { return (c==0||c==4)? Integer.class : String.class; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(26);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        refreshCourses(model);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(12,12,12,12));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton add = styledActionButton("+ Add");
        JButton del = styledActionButton("Delete");
        JButton save = styledActionButton("Save");
        actions.add(add); actions.add(del); actions.add(save);

        add.addActionListener(e -> openAddCourseDialog(model));
        del.addActionListener(e -> askDeleteCourse(model));
        save.addActionListener(e -> saveAllCourses(model));

        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        activateTable(table, sorter);

        return panel;
    }

    private void refreshCourses(DefaultTableModel model) {
        model.setRowCount(0);
        for (Course c : courseDAO.listAll()) {
            model.addRow(new Object[]{
                    c.getCourseId(),
                    c.getCode(),
                    c.getTitle(),
                    c.getCredits()
            });
        }
    }

    private void askDeleteCourse(DefaultTableModel model) {
        String idStr = JOptionPane.showInputDialog(this,
                "Enter COURSE_ID to delete:");

        if (idStr == null || idStr.isBlank()) return;

        try {
            int courseId = Integer.parseInt(idStr.trim());
            deleteCourse(courseId);
            refreshCourses(model);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Course ID");
        }
    }

    private void deleteCourse(int cid) {
        try (Connection con = DBConfig.getErpDataSource().getConnection()) {

            con.prepareStatement(
                    "DELETE g FROM grades g JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                            "JOIN sections s ON e.section_id = s.section_id WHERE s.course_id = " + cid
            ).executeUpdate();

            con.prepareStatement(
                    "DELETE e FROM enrollments e JOIN sections s ON e.section_id = s.section_id WHERE s.course_id = " + cid
            ).executeUpdate();

            con.prepareStatement("DELETE FROM sections WHERE course_id = " + cid).executeUpdate();
            con.prepareStatement("DELETE FROM courses WHERE course_id = " + cid).executeUpdate();

            JOptionPane.showMessageDialog(this, "Course deleted successfully.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void openAddCourseDialog(DefaultTableModel model) {
        JTextField code = new JTextField();
        JTextField title = new JTextField();
        JTextField credits = new JTextField();

        Object[] msg = {
                "Code:", code,
                "Title:", title,
                "Credits:", credits
        };

        if (JOptionPane.showConfirmDialog(this, msg, "Add Course",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

            try {
                Course c = new Course();
                c.setCode(code.getText());
                c.setTitle(title.getText());
                c.setCredits(Integer.parseInt(credits.getText()));

                boolean ok = courseDAO.create(c) > 0;   // returns generated ID

                if (ok) {
                    JOptionPane.showMessageDialog(this, "Course Added Successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add course!");
                }

                refreshCourses(model);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "ERROR: " + ex.getMessage());
            }
        }
    }


    private JPanel buildSectionsPanel() {
        String[] cols = {
                "Section ID", "Course ID", "Instructor ID",
                "Day/Time", "Room", "Capacity",
                "Semester", "Year"
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c != 0; // section_id not editable
            }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) {
                    case 0, 1, 5, 7 -> Integer.class;
                    case 2 -> Integer.class;
                    default -> String.class;
                };
            }
        };


        JTable table = new JTable(model);
        table.setRowHeight(26);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        refreshSections(model);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(12,12,12,12));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton add = styledActionButton("+ Add");
        JButton del = styledActionButton("Delete");
        JButton save = styledActionButton("Save");
        actions.add(add); actions.add(del); actions.add(save);

        add.addActionListener(e -> openAddSectionDialog(model));
        del.addActionListener(e -> askDeleteSection(model));
        save.addActionListener(e -> saveAllSections(model));

        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        activateTable(table, sorter);

        return panel;
    }
    private void saveAllSections(DefaultTableModel m) {

        for (int row = 0; row < m.getRowCount(); row++) {

            Section s = new Section();

            s.setSectionId((int) m.getValueAt(row, 0));
            s.setCourseId((int) m.getValueAt(row, 1));

            Object instObj = m.getValueAt(row, 2);
            if (instObj == null || instObj.toString().isBlank())
                s.setInstructorId(null);
            else
                s.setInstructorId(Integer.parseInt(instObj.toString()));

            Object dayObj = m.getValueAt(row, 3);
            s.setDayTime(dayObj == null ? "" : dayObj.toString());
            Object roomObj = m.getValueAt(row, 4);
            s.setRoom(roomObj == null ? "" : roomObj.toString());

            Object capObj = m.getValueAt(row, 5);
            int capacity = 0;
            if (capObj != null && !capObj.toString().isBlank()) {
                capacity = Integer.parseInt(capObj.toString());
            }
            s.setCapacity(capacity);

            Object semObj = m.getValueAt(row, 6);
            s.setSemester(semObj == null ? "" : semObj.toString());

            Object yearObj = m.getValueAt(row, 7);
            int year = 0;
            if (yearObj != null && !yearObj.toString().isBlank()) {
                year = Integer.parseInt(yearObj.toString());
            }
            s.setYear(year);
            sectionDAO.update(s);
        }

        JOptionPane.showMessageDialog(this, "ALL SECTIONS SAVED");
    }


    private void saveAllCourses(DefaultTableModel m) {
        for (int row = 0; row < m.getRowCount(); row++) {

            int id = (int) m.getValueAt(row, 0);
            String code = m.getValueAt(row, 1).toString();
            String title = m.getValueAt(row, 2).toString();
            int credits = Integer.parseInt(m.getValueAt(row, 3).toString());

            courseDAO.update(id, code, title, credits);
        }

        JOptionPane.showMessageDialog(this, "ALL COURSES SAVED");
    }
    private void refreshSections(DefaultTableModel model) {
        model.setRowCount(0);

        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT section_id, course_id, instructor_id, day_time, room, capacity, semester, year FROM sections ORDER BY section_id"
             );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("section_id"),
                        rs.getInt("course_id"),
                        rs.getObject("instructor_id"),
                        rs.getString("day_time"),
                        rs.getString("room"),
                        rs.getInt("capacity"),
                        rs.getString("semester"),
                        rs.getInt("year")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void askDeleteSection(DefaultTableModel model) {
        String idStr = JOptionPane.showInputDialog(this,
                "Enter SECTION_ID to delete:");

        if (idStr == null || idStr.isBlank()) return;

        try {
            int sectionId = Integer.parseInt(idStr.trim());
            deleteSection(sectionId);
            refreshSections(model);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Section ID");
        }
    }

    private void deleteSection(int sid) {
        try (Connection con = DBConfig.getErpDataSource().getConnection()) {

            con.prepareStatement(
                    "DELETE g FROM grades g JOIN enrollments e ON g.enrollment_id = e.enrollment_id WHERE e.section_id = " + sid
            ).executeUpdate();

            con.prepareStatement("DELETE FROM enrollments WHERE section_id = " + sid).executeUpdate();
            con.prepareStatement("DELETE FROM sections WHERE section_id = " + sid).executeUpdate();

            JOptionPane.showMessageDialog(this, "Section deleted successfully.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void openAddSectionDialog(DefaultTableModel model) {
        JTextField courseId = new JTextField();
        JTextField instructorId = new JTextField();
        JTextField dayTime = new JTextField();
        JTextField room = new JTextField();
        JTextField capacity = new JTextField();
        JTextField semester = new JTextField();
        JTextField year = new JTextField();

        Object[] msg = {
                "Course ID:", courseId,
                "Instructor User ID:", instructorId,
                "Day/Time:", dayTime,
                "Room:", room,
                "Capacity:", capacity,
                "Semester:", semester,
                "Year:", year
        };

        if (JOptionPane.showConfirmDialog(this, msg, "Create Section",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) VALUES (?,?,?,?,?,?,?)"
             )) {

            ps.setInt(1, Integer.parseInt(courseId.getText()));
            ps.setInt(2, Integer.parseInt(instructorId.getText()));
            ps.setString(3, dayTime.getText());
            ps.setString(4, room.getText());
            ps.setInt(5, Integer.parseInt(capacity.getText()));
            ps.setString(6, semester.getText());
            ps.setInt(7, Integer.parseInt(year.getText()));
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Section created!");
            refreshSections(model);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }


    private JPanel buildSettingsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(30, 30, 30, 30));

        JButton toggleMaint = new JButton("Toggle Maintenance Mode");
        toggleMaint.setAlignmentX(Component.CENTER_ALIGNMENT);
        toggleMaint.setBackground(ACTION_BG);
        toggleMaint.setForeground(Color.WHITE);

        JLabel maintLabel = new JLabel();
        maintLabel.setFont(new Font("Arial", Font.BOLD, 16));
        maintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        updateMaintenanceLabel(maintLabel);

        toggleMaint.addActionListener(e -> {
            AccessControl.toggleMaintenance();
            updateMaintenanceLabel(maintLabel);
        });

        JLabel dlLabel = new JLabel("Set Course Registration Deadline:");
        dlLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        DatePicker datePicker = new DatePicker();
        TimePicker timePicker = new TimePicker();

        datePicker.setMaximumSize(new Dimension(250, 30));
        timePicker.setMaximumSize(new Dimension(250, 30));

        String currentDl = AccessControl.getRegistrationDeadline();
        if (currentDl != null && !currentDl.isBlank()) {
            try {
                LocalDateTime dt = LocalDateTime.parse(currentDl);
                datePicker.setDate(dt.toLocalDate());
                timePicker.setTime(dt.toLocalTime());
            } catch (Exception ignored) {}
        }

        JLabel formatHint = new JLabel("Pick date + time from dropdown");
        formatHint.setFont(new Font("Arial", Font.PLAIN, 12));
        formatHint.setForeground(Color.GRAY);
        formatHint.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton saveDl = new JButton("Save Deadline");
        saveDl.setBackground(ACTION_BG);
        saveDl.setForeground(Color.WHITE);
        saveDl.setAlignmentX(Component.CENTER_ALIGNMENT);

        saveDl.addActionListener(e -> {
            var date = datePicker.getDate();
            var time = timePicker.getTime();

            if (date == null || time == null) {
                JOptionPane.showMessageDialog(this,
                        "Please select BOTH date and time.",
                        "Missing values",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDateTime dt = LocalDateTime.of(date, time);
            AccessControl.setRegistrationDeadline(dt.toString());

            JOptionPane.showMessageDialog(this,
                    "Deadline saved:\n" + dt.toString());
        });

        p.add(toggleMaint);
        p.add(Box.createRigidArea(new Dimension(0, 20)));
        p.add(maintLabel);

        p.add(Box.createRigidArea(new Dimension(0, 40)));
        p.add(dlLabel);

        p.add(datePicker);
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        p.add(timePicker);

        p.add(formatHint);
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        p.add(saveDl);

        return p;
    }


    private void updateMaintenanceLabel(JLabel lbl) {
        if (AccessControl.isMaintenanceOn()) {
            lbl.setText("MAINTENANCE MODE ACTIVE");
            lbl.setForeground(Color.RED);
        } else {
            lbl.setText("System Running Normally");
            lbl.setForeground(new Color(0, 128, 0)); // darker green
        }
    }

    private JButton styledActionButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACTION_BG);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(ACTION_BG.darker()); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(ACTION_BG); }
        });
        return b;
    }

    private void changeAdminPassword() {
        JPasswordField oldP = new JPasswordField(), newP = new JPasswordField(), confP = new JPasswordField();
        Object[] msg = {"Old Password:", oldP, "New Password:", newP, "Confirm New:", confP};
        int opt = JOptionPane.showConfirmDialog(this, msg, "Change Password", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;
        String old = new String(oldP.getPassword()), nw = new String(newP.getPassword()), cf = new String(confP.getPassword());
        if (!nw.equals(cf)) { JOptionPane.showMessageDialog(this, "Mismatch"); return; }
        try (Connection c = DBConfig.getAuthDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users_auth WHERE user_id=?")) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { JOptionPane.showMessageDialog(this, "Admin not found"); return; }
                if (!PasswordHash.verify(old, rs.getString(1))) { JOptionPane.showMessageDialog(this, "Old incorrect"); return; }
            }
            try (PreparedStatement up = c.prepareStatement("UPDATE users_auth SET password_hash=? WHERE user_id=?")) {
                up.setString(1, PasswordHash.hash(nw)); up.setInt(2, adminId); up.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Password changed");
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }
    private void activateCurrentTab() {
        Component c = centerPanel.getComponents()[0];
        for (Component cc : centerPanel.getComponents()) {
            if (cc.isVisible()) {
                c = cc;
                break;
            }
        }
        JTable table = null;
        TableRowSorter<DefaultTableModel> sorter = null;

        for (Component child : ((JPanel) c).getComponents()) {
            if (child instanceof JScrollPane sp) {
                Component view = sp.getViewport().getView();
                if (view instanceof JTable t) {
                    table = t;
                    sorter = (TableRowSorter<DefaultTableModel>) t.getRowSorter();
                }
            }
        }

        if (table != null && sorter != null) {
            activateTable(table, sorter);
        }
    }
}

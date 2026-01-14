package edu.univ.erp.ui.instructor;
import edu.univ.erp.access.AccessControl;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
import edu.univ.erp.util.MathEvaluator;
import edu.univ.erp.ui.auth.LoginFrame;
import edu.univ.erp.auth.PasswordHash;
import edu.univ.erp.config.DBConfig;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class InstructorDashboard extends JFrame {
    private final int instructorId;
    private String username = "Instructor";
    private final SectionDAO sectionDAO = new SectionDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final GradeDAO gradeDAO = new GradeDAO();
    private final Color SIDEBAR_BG = Color.decode("#D65D72");
    private final Color SIDEBAR_HOVER = Color.decode("#EEA8B4");
    private final Color CARD_DEFAULT = Color.decode("#EEA8B4");
    private final Color CARD_HOVER = Color.decode("#DA7385");
    private final Color ACTION_BG = Color.decode("#DA7385");
    private final Color WHITE = Color.WHITE;
    private final CardLayout cards = new CardLayout();
    private final JPanel centerPanel = new JPanel(cards);
    private JTable activeTable;
    private TableRowSorter<DefaultTableModel> activeSorter;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sectionsSorter;
    private TableRowSorter<DefaultTableModel> studentsSorter;
    private JTable statsTable;
    private TableRowSorter<DefaultTableModel> statsSorter;
    private JTable sectionsTable;
    private JTable studentsTable;
    private JTextField customRuleField;
    private Map<Integer, String[]> courseLookup = new HashMap<>();

    public InstructorDashboard(int instructorId) {
        this.instructorId = instructorId;
        fetchUsername();
        loadCourseNames();
        setTitle("University ERP");
        setSize(1200, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildLeftSidebar(), BorderLayout.WEST);
        add(buildTopBar(), BorderLayout.NORTH);

        centerPanel.add(buildHomePanel(), "HOME");
        centerPanel.add(buildSectionsPanel(), "MY_SECTIONS");
        centerPanel.add(buildGradesPanel(), "ENTER_GRADES");
        centerPanel.add(buildStatsPanel(), "CLASS_STATS");

        add(centerPanel, BorderLayout.CENTER);
        cards.show(centerPanel, "HOME");

        refreshSectionsTableModel();
    }

    private void loadCourseNames() {
        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT course_id, code, title FROM courses")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    courseLookup.put(
                            rs.getInt("course_id"),
                            new String[]{rs.getString("code"), rs.getString("title")}
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Course name load error: " + e.getMessage());
        }
    }

    private void fetchUsername() {
        try (Connection con = DBConfig.getAuthDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT username FROM users_auth WHERE user_id=?")) {
            ps.setInt(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) username = rs.getString(1);
            }
        } catch (Exception ignored) {
        }
    }

    private JPanel buildLeftSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(SIDEBAR_BG);
        side.setPreferredSize(new Dimension(180, 0));

        JPanel items = new JPanel();
        items.setOpaque(false);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBorder(new EmptyBorder(12, 8, 12, 8));

        items.add(makeSideButton("", "Home", e -> {
            cards.show(centerPanel, "HOME");
            updateActiveTableForCard("HOME");
            applySearch(searchField == null ? "" : searchField.getText());
        }));
        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(makeSideButton("", "My Sections", e -> {
            refreshSectionsTableModel();
            cards.show(centerPanel, "MY_SECTIONS");
            updateActiveTableForCard("MY_SECTIONS");
            applySearch(searchField == null ? "" : searchField.getText());
        }));
        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(makeSideButton("", "Enter Grades", e -> {
            cards.show(centerPanel, "ENTER_GRADES");
            updateActiveTableForCard("ENTER_GRADES");
            applySearch(searchField == null ? "" : searchField.getText());
        }));
        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(makeSideButton("", "Class Stats", e -> {
            refreshStatsTable();
            cards.show(centerPanel, "CLASS_STATS");
            updateActiveTableForCard("CLASS_STATS");
            applySearch(searchField == null ? "" : searchField.getText());
        }));

        items.add(Box.createVerticalGlue());
        side.add(items, BorderLayout.NORTH);

        JPanel prof = new JPanel(new BorderLayout());
        prof.setOpaque(false);
        prof.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel name = new JLabel("<html><b>" + username + "</b><br/><small>Instructor</small></html>");
        name.setForeground(WHITE);
        prof.add(name, BorderLayout.CENTER);

        side.add(prof, BorderLayout.SOUTH);
        return side;
    }

    private void refreshStatsTable() {
        DefaultTableModel m = (DefaultTableModel) statsTable.getModel();
        m.setRowCount(0);

        try {
            List<Section> sections = sectionDAO.listAll();
            for (Section s : sections) {
                if (s.getInstructorId() == null || s.getInstructorId() != instructorId)
                    continue;

                int sectionId = s.getSectionId();
                int courseId = s.getCourseId();
                String code = courseLookup.get(courseId)[0];
                String title = courseLookup.get(courseId)[1];

                Map<String, Double> avg = getAverages(sectionId);

                m.addRow(new Object[]{
                        String.valueOf(sectionId),
                        code,
                        title,
                        avg.get("quiz"),
                        avg.get("midterm"),
                        avg.get("endsem"),
                        avg.get("assignment"),
                        avg.get("finalScore")
                });
            }

            statsSorter = new TableRowSorter<>(m);
            statsTable.setRowSorter(statsSorter);
            activateTable(statsTable, statsSorter);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load stats: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Map<String, Double> getAverages(int sectionId) {
        Map<String, Double> out = new HashMap<>();
        out.put("quiz", 0.0);
        out.put("midterm", 0.0);
        out.put("endsem", 0.0);
        out.put("assignment", 0.0);
        out.put("finalScore", 0.0);

        try (Connection con = DBConfig.getErpDataSource().getConnection()) {

            String sql =
                    "SELECT g.component, g.score " +
                            "FROM grades g " +
                            "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                            "WHERE e.section_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();

            Map<String, List<Double>> bucket = new HashMap<>();
            bucket.put("quiz", new ArrayList<>());
            bucket.put("midterm", new ArrayList<>());
            bucket.put("endsem", new ArrayList<>());
            bucket.put("assignment", new ArrayList<>());
            bucket.put("final", new ArrayList<>());

            while (rs.next()) {
                String comp = rs.getString("component").toLowerCase();
                double score = rs.getDouble("score");

                if (bucket.containsKey(comp)) bucket.get(comp).add(score);
                if (comp.equals("final")) bucket.get("final").add(score);
            }

            for (String key : bucket.keySet()) {
                List<Double> list = bucket.get(key);
                if (!list.isEmpty()) {
                    double avg = list.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    if (key.equals("final")) out.put("finalScore", avg);
                    else out.put(key, avg);
                }
            }

        } catch (Exception ignored) {
        }

        return out;
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
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(SIDEBAR_HOVER);
                b.setForeground(Color.BLACK);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(SIDEBAR_BG);
                b.setForeground(WHITE);
            }
        });
        return b;
    }


    private JPanel buildStatsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] cols = {
                "Section ID", "Course Code", "Course Title",
                "Avg Quiz", "Avg Mid", "Avg End", "Avg Assign", "Avg Final"
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return (col <= 2) ? String.class : Double.class;
            }
        };

        statsTable = new JTable(model);
        statsTable.setRowHeight(26);

        statsSorter = new TableRowSorter<>(model);
        statsTable.setRowSorter(statsSorter);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton refresh = styledActionButton("Refresh Stats");
        refresh.addActionListener(e -> refreshStatsTable());
        actions.add(refresh);

        p.add(actions, BorderLayout.NORTH);
        p.add(new JScrollPane(statsTable), BorderLayout.CENTER);

        return p;
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("Instructor Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(360, 28));
        searchField.setToolTipText("Search active table...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applySearch(searchField.getText());
            }
        });

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
        right.add(searchField);
        right.add(profileBtn);

        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private void showProfilePopup(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem change = new JMenuItem("Change Password");
        JMenuItem logout = new JMenuItem("Logout");
        change.addActionListener(e -> changePassword());
        logout.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });
        menu.add(change);
        menu.add(logout);
        menu.show(invoker, 0, invoker.getHeight());
    }

    private void changePassword() {
        JPasswordField oldP = new JPasswordField();
        JPasswordField newP = new JPasswordField();
        JPasswordField confP = new JPasswordField();
        Object[] msg = {"Old Password:", oldP, "New Password:", newP, "Confirm New:", confP};
        if (JOptionPane.showConfirmDialog(this, msg, "Change Password", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;

        String oldPass = new String(oldP.getPassword());
        String newPass = new String(newP.getPassword());
        String confPass = new String(confP.getPassword());
        if (!newPass.equals(confPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do NOT match!");
            return;
        }

        try (Connection con = DBConfig.getAuthDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT password_hash FROM users_auth WHERE user_id=?");
            ps.setInt(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "User not found");
                    return;
                }
                if (!PasswordHash.verify(oldPass, rs.getString(1))) {
                    JOptionPane.showMessageDialog(this, "Old password incorrect!");
                    return;
                }
            }
            PreparedStatement up = con.prepareStatement("UPDATE users_auth SET password_hash=? WHERE user_id=?");
            up.setString(1, PasswordHash.hash(newPass));
            up.setInt(2, instructorId);
            up.executeUpdate();
            JOptionPane.showMessageDialog(this, "Password Updated!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }


    private void applySearch(String txt) {
        if (activeSorter == null) return;
        if (txt == null || txt.trim().isEmpty()) {
            activeSorter.setRowFilter(null);
        } else {
            activeSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(txt)));
        }
    }

    private void exportCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Grades to CSV");

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                DefaultTableModel m = (DefaultTableModel) studentsTable.getModel();

                // write header row
                for (int c = 0; c < m.getColumnCount(); c++) {
                    pw.print(m.getColumnName(c));
                    if (c < m.getColumnCount() - 1) pw.print(",");
                }
                pw.println();

                // write table rows
                for (int r = 0; r < m.getRowCount(); r++) {
                    for (int c = 0; c < m.getColumnCount(); c++) {
                        Object val = m.getValueAt(r, c);
                        pw.print(val == null ? "" : val.toString());
                        if (c < m.getColumnCount() - 1) pw.print(",");
                    }
                    pw.println();
                }

                JOptionPane.showMessageDialog(this, "CSV exported successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        }
    }

    private void importCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Grades from CSV");

        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        try (BufferedReader br = new BufferedReader(new FileReader(fc.getSelectedFile()))) {

            DefaultTableModel m = (DefaultTableModel) studentsTable.getModel();
            List<String> errors = new ArrayList<>();

            // Build lookup: enrollment_id → row index
            Map<Integer, Integer> rowByEnrollId = new HashMap<>();
            for (int i = 0; i < m.getRowCount(); i++) {
                int eid = toInt(m.getValueAt(i, 0));
                rowByEnrollId.put(eid, i);

            }

            String header = br.readLine();
            if (header == null)
                throw new Exception("File is empty!");

            String line;
            int csvLineNum = 1;

            while ((line = br.readLine()) != null) {
                csvLineNum++;
                String[] parts = line.split(",");

                if (parts.length < 8) {
                    errors.add("Line " + csvLineNum + ": Not enough columns");
                    continue;
                }

                try {
                    int enrollId = toInt(parts[0].trim());

                    if (!rowByEnrollId.containsKey(enrollId)) {
                        errors.add("Line " + csvLineNum +
                                ": Enrollment ID " + enrollId + " does NOT belong to this section.");
                        continue;
                    }

                    int tableRow = rowByEnrollId.get(enrollId);

                    for (int c = 0; c < 8; c++) {
                        String v = parts[c].trim();

                        if (v.isEmpty()) {
                            m.setValueAt(null, tableRow, c);
                            continue;
                        }


                        if (c >= 3 && c <= 6) {
                            m.setValueAt(Double.parseDouble(v), tableRow, c);
                        } else {
                            m.setValueAt(v, tableRow, c);
                        }
                    }

                } catch (NumberFormatException ex) {
                    errors.add("Line " + csvLineNum + ": Invalid enrollment_id or number format");
                } catch (Exception ex) {
                    errors.add("Line " + csvLineNum + ": " + ex.getMessage());
                }
            }

            if (!errors.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Import completed with warnings:\n\n" + String.join("\n", errors));
            } else {
                JOptionPane.showMessageDialog(this, "CSV imported successfully!");
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage());
        }
    }

    private int toInt(Object o) {
        if (o == null) return -1;
        if (o instanceof Integer) return (Integer) o;
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    private void activateTable(JTable table, TableRowSorter<DefaultTableModel> sorter) {
        this.activeTable = table;
        this.activeSorter = sorter;
        applySearch(searchField == null ? "" : searchField.getText());
    }

    private void updateActiveTableForCard(String cardName) {
        if (cardName.equals("ENTER_GRADES")) {
            boolean m = AccessControl.isMaintenanceOn();
            studentsTable.setEnabled(!m);
        }

        switch (cardName) {
            case "MY_SECTIONS":
                if (sectionsTable != null && sectionsTable.getRowSorter() instanceof TableRowSorter) {
                    sectionsSorter = (TableRowSorter<DefaultTableModel>) sectionsTable.getRowSorter();
                    activateTable(sectionsTable, sectionsSorter);
                } else {
                    activeSorter = null;
                    activeTable = null;
                }
                break;
            case "ENTER_GRADES":
                if (studentsTable != null && studentsTable.getRowSorter() instanceof TableRowSorter) {
                    studentsSorter = (TableRowSorter<DefaultTableModel>) studentsTable.getRowSorter();
                    activateTable(studentsTable, studentsSorter);
                } else {
                    activeSorter = null;
                    activeTable = null;
                }
                break;
            case "CLASS_STATS":
                if (statsTable != null && statsTable.getRowSorter() instanceof TableRowSorter) {
                    statsSorter = (TableRowSorter<DefaultTableModel>) statsTable.getRowSorter();
                    activateTable(statsTable, statsSorter);
                }
                break;

            default:
                activeSorter = null;
                activeTable = null;
                break;
        }
    }


    private JPanel buildHomePanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 28, 28));
        p.setBackground(Color.WHITE);

        JPanel mySectionsCard = makeBigCard("My Sections", this::countMySections, "MY_SECTIONS");
        JPanel totalStudentsCard = makeBigCard("Total Students (All my sections)", this::countMyStudents, "MY_SECTIONS");
        JPanel pendingGradesCard = makeBigCard("Enrollments (for grading)", this::countPendingGrades, "ENTER_GRADES");

        p.add(mySectionsCard);
        p.add(totalStudentsCard);
        p.add(pendingGradesCard);
        return p;
    }

    private JPanel makeBigCard(String title, CountSupplier supplier, String nav) {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(260, 140));
        card.setBackground(CARD_DEFAULT);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));
        JLabel v = new JLabel(String.valueOf(supplier.get()), SwingConstants.CENTER);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 32f));

        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(CARD_HOVER);
                card.revalidate();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(CARD_DEFAULT);
                card.revalidate();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                cards.show(centerPanel, nav);
                if ("MY_SECTIONS".equals(nav)) refreshSectionsTableModel();
                updateActiveTableForCard(nav);
                applySearch(searchField == null ? "" : searchField.getText());
            }
        });

        return card;
    }

    private interface CountSupplier {
        int get();
    }

    private int countMySections() {
        int cnt = 0;
        try {
            for (Section s : sectionDAO.listAll())
                if (s.getInstructorId() != null && s.getInstructorId() == instructorId) cnt++;
        } catch (Exception ignored) {
        }
        return cnt;
    }

    private int countMyStudents() {
        int total = 0;
        try {
            for (Section s : sectionDAO.listAll()) {
                if (s.getInstructorId() != null && s.getInstructorId() == instructorId) {
                    total += enrollmentDAO.listStudentIdsForSection(s.getSectionId()).size();
                }
            }
        } catch (Exception ignored) {
        }
        return total;
    }

    private int countPendingGrades() {
        return countMyStudents();
    }


    private JPanel buildSectionsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] cols = {"Section ID", "Course Code", "Course Title", "Capacity"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        sectionsTable = new JTable(model);
        sectionsTable.setRowHeight(26);

        sectionsSorter = new TableRowSorter<>(model);
        sectionsTable.setRowSorter(sectionsSorter);
        activateTable(sectionsTable, sectionsSorter);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton loadStudentsBtn = styledActionButton("Load Students for Selected");
        JButton refreshBtn = styledActionButton("Refresh Sections");
        actions.add(loadStudentsBtn);
        actions.add(refreshBtn);

        loadStudentsBtn.addActionListener(e -> {
            int viewRow = sectionsTable.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Select a section first.");
                return;
            }
            int modelRow = sectionsTable.convertRowIndexToModel(viewRow);
            int sectionId = (int) sectionsTable.getModel().getValueAt(modelRow, 0);
            loadStudentsForSection(sectionId);
            cards.show(centerPanel, "ENTER_GRADES");
            updateActiveTableForCard("ENTER_GRADES");
            applySearch(searchField == null ? "" : searchField.getText());
        });

        refreshBtn.addActionListener(e -> {
            refreshSectionsTableModel();
            updateActiveTableForCard("MY_SECTIONS");
            applySearch(searchField == null ? "" : searchField.getText());
        });

        p.add(actions, BorderLayout.NORTH);
        p.add(new JScrollPane(sectionsTable), BorderLayout.CENTER);
        return p;
    }

    private void refreshSectionsTableModel() {
        DefaultTableModel model = (DefaultTableModel) sectionsTable.getModel();
        model.setRowCount(0);

        try {
            for (Section s : sectionDAO.listAll()) {
                if (s.getInstructorId() != null && s.getInstructorId() == instructorId) {
                    String code = "N/A", title = "N/A";
                    if (courseLookup.containsKey(s.getCourseId())) {
                        code = courseLookup.get(s.getCourseId())[0];
                        title = courseLookup.get(s.getCourseId())[1];
                    }
                    model.addRow(new Object[]{s.getSectionId(), code, title, s.getCapacity()});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load sections: " + e.getMessage());
        }
    }
    private boolean maintenanceCheck() {
        if (AccessControl.isMaintenanceOn()) {
            JOptionPane.showMessageDialog(
                    this,
                    "System is in Maintenance Mode.\nInstructors cannot modify grades right now.",
                    "Maintenance Active",
                    JOptionPane.WARNING_MESSAGE
            );
            return true;
        }
        return false;
    }

    private JPanel buildGradesPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] cols = {"Enrollment ID", "Student ID", "Roll Number", "Quiz", "Midterm", "Endterm", "Assignment", "Final Grade"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        studentsTable = new JTable(model);
        studentsTable.setRowHeight(26);

        studentsSorter = new TableRowSorter<>(model);
        studentsTable.setRowSorter(studentsSorter);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton saveBtn = styledActionButton("Save Scores");
        JButton computeBtn = styledActionButton("Compute Final Grade");
        JButton exportBtn = styledActionButton("Export CSV");
        JButton importBtn = styledActionButton("Import CSV");

        topActions.add(saveBtn);
        topActions.add(computeBtn);
        topActions.add(exportBtn);
        topActions.add(importBtn);

        saveBtn.addActionListener(e -> {
            if (maintenanceCheck()) return;
            saveScores();
        });

        computeBtn.addActionListener(e -> {
            if (maintenanceCheck()) return;
            computeFinalGrade();
        });

        exportBtn.addActionListener(e -> {
            if (maintenanceCheck()) return;
            exportCSV();
        });

        importBtn.addActionListener(e -> {
            if (maintenanceCheck()) return;
            importCSV();
        });



        p.add(topActions, BorderLayout.NORTH);
        p.add(new JScrollPane(studentsTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("Formula:"));
        customRuleField = new JTextField(45);
        customRuleField.setText("0.25*quiz + 0.25*midterm + 0.30*endsem + 0.20*assignment");
        bottom.add(customRuleField);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    private void loadStudentsForSection(int sectionId) {
        DefaultTableModel m = (DefaultTableModel) studentsTable.getModel();
        m.setRowCount(0);

        List<Integer> sidList = enrollmentDAO.listStudentIdsForSection(sectionId);
        for (int sid : sidList) {
            int eid = enrollmentDAO.getEnrollmentId(sid, sectionId);
            Student st = studentDAO.findByUserId(sid);
            Map<String, Double> g = getScores(eid);
            String fin = getFinal(eid);
            m.addRow(new Object[]{
                    eid,
                    sid,
                    st.getRollNo(),
                    g.get("quiz"),
                    g.get("midterm"),
                    g.get("endsem"),
                    g.get("assignment"),
                    fin
            });
        }

        if (studentsTable.getRowSorter() == null) {
            studentsSorter = new TableRowSorter<>((DefaultTableModel) studentsTable.getModel());
            studentsTable.setRowSorter(studentsSorter);
        } else if (studentsTable.getRowSorter() instanceof TableRowSorter) {
            studentsSorter = (TableRowSorter<DefaultTableModel>) studentsTable.getRowSorter();
        }
        activateTable(studentsTable, studentsSorter);
    }

    private Map<String, Double> getScores(int eid) {
        Map<String, Double> map = new HashMap<>();
        map.put("quiz", null);
        map.put("midterm", null);
        map.put("endsem", null);
        map.put("assignment", null);

        gradeDAO.findByEnrollment(eid).forEach(g -> {
            String c = g.getComponent().toLowerCase();
            if (map.containsKey(c)) map.put(c, g.getScore());
        });
        return map;
    }

    private String getFinal(int eid) {
        return gradeDAO.findByEnrollment(eid).stream().map(g -> g.getFinalGrade()).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private void saveScores() {
        if (AccessControl.isMaintenanceOn()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot save. Maintenance mode is ON.",
                    "Maintenance Active",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultTableModel m = (DefaultTableModel) studentsTable.getModel();

        for (int i = 0; i < m.getRowCount(); i++) {
            int eid = toInt(m.getValueAt(i, 0));
            if (eid <= 0) continue;
            int sid = toInt(m.getValueAt(i, 1));
            for (String comp : List.of("quiz", "midterm", "endsem", "assignment")) {
                int col = compIndex(comp);
                Object v = m.getValueAt(i, col);

                if (v != null) {
                    Double num = toDouble(v);
                    gradeDAO.save(eid, comp, num);
                }
            }

            Object fin = m.getValueAt(i, 7);
            if (fin != null) {
                gradeDAO.save(eid, "final", toDouble(fin));
                gradeDAO.setFinalGrade(eid, fin.toString());
            }
        }

        JOptionPane.showMessageDialog(this, "Scores Saved!");
    }

    private int compIndex(String c) {
        return switch (c) {
            case "quiz" -> 3;
            case "midterm" -> 4;
            case "endsem" -> 5;
            default -> 6;
        };
    }

    private void computeFinalGrade() {
        if (AccessControl.isMaintenanceOn()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot compute grades. Maintenance mode is ON.",
                    "Maintenance Active",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String f = customRuleField.getText().trim();
        DefaultTableModel m = (DefaultTableModel) studentsTable.getModel();

        for (int i = 0; i < m.getRowCount(); i++) {
            String expr = f;
            for (String c : List.of("quiz", "midterm", "endsem", "assignment")) {
                Object v = m.getValueAt(i, compIndex(c));
                if (v == null) {
                    JOptionPane.showMessageDialog(this, "Some score is empty");
                    return;
                }
                expr = expr.replace(c, v.toString());
            }
            double val = MathEvaluator.eval(expr);
            m.setValueAt(String.format("%.2f", val), i, 7);
        }
        JOptionPane.showMessageDialog(this, "Final Grades Computed!");
    }

    private Double toDouble(Object o) {
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private JButton styledActionButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACTION_BG);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(ACTION_BG.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(ACTION_BG);
            }
        });
        return b;
    }
}
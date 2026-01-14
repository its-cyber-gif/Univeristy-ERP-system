package edu.univ.erp.ui.student;
import edu.univ.erp.ui.auth.LoginFrame;
import edu.univ.erp.auth.PasswordHash;
import edu.univ.erp.config.DBConfig;
import edu.univ.erp.service.StudentService;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
public class StudentDashboard extends JFrame {

    private final int studentId;
    private String username = "Student";
    private final StudentService service = new StudentService();

    private final Color SIDEBAR_BG = Color.decode("#D65D72");
    private final Color SIDEBAR_HOVER = Color.decode("#EEA8B4");
    private final Color CARD_DEFAULT = Color.decode("#EEA8B4");
    private final Color CARD_HOVER = Color.decode("#DA7385");
    private final Color ACTION_BG = Color.decode("#DA7385");
    private final Color WHITE = Color.WHITE;

    private CardLayout cards = new CardLayout();
    private JPanel centerPanel = new JPanel(cards);

    private JTable activeTable;
    private TableRowSorter<DefaultTableModel> activeSorter;
    private JTextField searchField;

    private JTable catalogTable;
    private JTable myRegTable;
    private JTable gradesTable;

    private TableRowSorter<DefaultTableModel> catalogSorter;
    private TableRowSorter<DefaultTableModel> myRegSorter;
    private TableRowSorter<DefaultTableModel> gradesSorter;

    public StudentDashboard(int studentId) {
        this.studentId = studentId;
        fetchUsername();

        setTitle("University ERP");
        setSize(1200, 760);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildTopBar(), BorderLayout.NORTH);

        centerPanel.add(buildHomePanel(), "HOME");
        centerPanel.add(buildCatalogPanel(), "CATALOG");
        centerPanel.add(buildMyRegPanel(), "MYREG");
        centerPanel.add(buildGradesPanel(), "GRADES");
        centerPanel.add(buildTimetablePanel(), "TIMETABLE");

        add(centerPanel, BorderLayout.CENTER);

        setActiveCard("HOME");
        refreshCatalog();
        refreshMyRegistrations();
        refreshGrades();
    }

    private void fetchUsername() {
        try (Connection con = DBConfig.getAuthDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT username FROM users_auth WHERE user_id=?")) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) username = rs.getString(1);
        } catch (Exception ignored) {}
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(SIDEBAR_BG);
        side.setPreferredSize(new Dimension(180, 0));

        JPanel items = new JPanel();
        items.setOpaque(false);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBorder(new EmptyBorder(12, 8, 12, 8));

        items.add(sideBtn("  Home", "HOME"));
        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(sideBtn("  Courses", "CATALOG"));
        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(sideBtn("  My Registrations", "MYREG"));
        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(sideBtn("  Grades", "GRADES"));
        items.add(Box.createRigidArea(new Dimension(0, 8)));
        items.add(sideBtn("  Timetable", "TIMETABLE"));
        items.add(Box.createVerticalGlue());

        side.add(items, BorderLayout.NORTH);
        JPanel prof = new JPanel(new BorderLayout());
        prof.setOpaque(false);
        prof.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel name = new JLabel("<html><b>" + username + "</b><br/><small>Student</small></html>");
        name.setForeground(WHITE);
        prof.add(name, BorderLayout.CENTER);
        side.add(prof, BorderLayout.SOUTH);
        return side;
    }

    private JButton sideBtn(String text, String card) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBackground(SIDEBAR_BG);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        b.addActionListener(e -> setActiveCard(card));

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(SIDEBAR_HOVER);
                b.setForeground(Color.BLACK);
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(SIDEBAR_BG);
                b.setForeground(WHITE);
            }
            @Override public void mousePressed(MouseEvent e) { b.setBackground(SIDEBAR_HOVER.darker()); }
            @Override public void mouseReleased(MouseEvent e) { b.setBackground(SIDEBAR_HOVER); }
        });
        return b;
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("Student Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(320, 30));
        searchField.setToolTipText("Search table...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                applySearch(searchField.getText());
            }
        });

        JButton bellBtn = new JButton("🔔");
        bellBtn.setPreferredSize(new Dimension(34, 34));
        bellBtn.setBackground(Color.WHITE);
        bellBtn.setFocusPainted(false);
        bellBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bellBtn.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        bellBtn.setToolTipText("Notifications");

        bellBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { bellBtn.setBackground(new Color(235,235,235)); }
            @Override public void mouseExited(MouseEvent e) { bellBtn.setBackground(Color.WHITE); }
        });

        bellBtn.addActionListener(e -> showNotifications(bellBtn));

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
        right.add(bellBtn);
        right.add(profileBtn);

        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        return top;
    }
    private void showNotifications(Component invoker) {
        JPopupMenu menu = new JPopupMenu();

        List<Map<String, Object>> list = service.getNotifications(studentId);
        if (list.isEmpty()) {
            JMenuItem none = new JMenuItem("No notifications");
            none.setEnabled(false);
            menu.add(none);
        } else {
            for (Map<String, Object> n : list) {

                java.sql.Timestamp ts = (java.sql.Timestamp) n.get("time");
                String formattedTime = "";
                if (ts != null) {
                    LocalDateTime dbTime = ts.toLocalDateTime();
                    LocalDateTime adjusted = dbTime.minusHours(5).minusMinutes(30);
                    formattedTime = adjusted.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
                }

                JMenuItem item = new JMenuItem(formattedTime + " — " + n.get("msg"));
                menu.add(item);
            }
        }

        menu.show(invoker, 0, invoker.getHeight());
    }

    private void setActiveCard(String card) {
        cards.show(centerPanel, card);
        switch (card) {
            case "CATALOG":
                activeTable = catalogTable;
                activeSorter = catalogSorter;
                break;
            case "MYREG":
                activeTable = myRegTable;
                activeSorter = myRegSorter;
                break;
            case "GRADES":
                activeTable = gradesTable;
                activeSorter = gradesSorter;
                break;
            case "TIMETABLE" :
                centerPanel.remove(centerPanel.getComponentZOrder(centerPanel)); // safe refresh
                centerPanel.add(buildTimetablePanel(), "TIMETABLE");
            default:
                activeTable = null;
                activeSorter = null;
                break;
        }

        applySearch(searchField == null ? "" : searchField.getText());
    }

    private void applySearch(String txt) {
        if (activeSorter == null) return;
        if (txt == null || txt.isBlank()) {
            activeSorter.setRowFilter(null);
            return;
        }
        activeSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(txt)));
    }

    private JPanel buildHomePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);

        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 24));
        cardsRow.setBackground(Color.WHITE);
        cardsRow.add(card("Available Courses", () -> service.getCourseCatalogForUI().size(), "CATALOG"));
        cardsRow.add(card("My Registrations", () -> service.getMyRegistrations(studentId).size(), "MYREG"));
        cardsRow.add(card("Grades", () -> service.getGrades(studentId).size(), "GRADES"));
        p.add(cardsRow);

        JPanel chartsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 24));
        chartsRow.setBackground(Color.WHITE);
        chartsRow.add(buildCreditsDonut());

        p.add(chartsRow);

        return p;
    }
    private ChartPanel buildCreditsDonut() {

        int completed = service.getCompletedCredits(studentId);
        int total = 156;

        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Completed", completed/5);
        dataset.setValue("Remaining", total - completed/5);

        JFreeChart chart = ChartFactory.createRingChart(
                "Credit Completion",
                dataset,
                false,
                true,
                false
        );

        RingPlot plot = (RingPlot) chart.getPlot();
        plot.setSectionPaint("Completed", new Color(214, 93, 114));  // green
        plot.setSectionPaint("Remaining", new Color(220, 220, 220)); // gray

        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);

        plot.setSimpleLabels(true);

        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1}"));
        plot.setInteriorGap(0.04);

        chart.setTitle(new TextTitle(
                completed/5 + " / " + total + " Credits",
                new Font("Segoe UI", Font.BOLD, 16)
        ));

        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(350, 300));

        return panel;
    }


    private JPanel card(String title, CountSupplier count, String nav) {
        JPanel c = new JPanel(new BorderLayout());
        c.setPreferredSize(new Dimension(240, 130));
        c.setBackground(CARD_DEFAULT);
        c.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 15f));

        JLabel v = new JLabel(String.valueOf(count.get()), SwingConstants.CENTER);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 32f));

        c.add(t, BorderLayout.NORTH);
        c.add(v, BorderLayout.CENTER);

        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { c.setBackground(CARD_HOVER); }
            @Override public void mouseExited(MouseEvent e) { c.setBackground(CARD_DEFAULT); }
            @Override public void mouseClicked(MouseEvent e) { setActiveCard(nav); }
        });

        return c;
    }

    private interface CountSupplier { int get(); }
    private JPanel buildTimetablePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(12,12,12,12));
        p.setBackground(Color.WHITE);

        String[] days = {"Time", "Mon", "Tue", "Wed", "Thu", "Fri"};
        String[][] grid = new String[10][6];  // 8 AM – 5 PM (10 rows)

        for (int i = 1; i < 10; i++) {
            grid[i][0] = (8 + i) + ":30";
        }

        List<Map<String, Object>> list = service.getMyRegistrations(studentId);

        for (Map<String, Object> r : list) {
            String slot = (String) r.get("day_time");  // e.g., "Mon/Wed 9:00-11:00"
            if (slot == null) continue;

            String title = (String) r.get("code");
            String[] parts = slot.split(" ");
            if (parts.length < 2) continue;

            String[] courseDays = parts[0].split("/");
            String[] timeParts = parts[1].split("-");
            int start = Integer.parseInt(timeParts[0].split(":")[0]);
            int end = Integer.parseInt(timeParts[1].split(":")[0]);

            for (String d : courseDays) {
                int col = switch (d.substring(0,3)) {
                    case "Mon" -> 1;
                    case "Tue" -> 2;
                    case "Wed" -> 3;
                    case "Thu" -> 4;
                    case "Fri" -> 5;
                    default -> -1;
                };
                if (col == -1) continue;

                for (int h = start; h < end; h++) {
                    int row = h - 8;
                    if (row >= 0 && row < 10) {
                        grid[row][col] = (grid[row][col] == null) ?
                                title :
                                grid[row][col] + " | " + title;
                    }
                }
            }
        }

        JTable t = new JTable(grid, days);
        t.setRowHeight(35);
        t.setEnabled(false);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCatalogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(12,12,12,12));

        String[] cols = {"Section ID", "Code", "Title", "Credits", "Capacity", "Instructor name"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        catalogTable = new JTable(model);
        catalogTable.setRowHeight(26);
        catalogSorter = new TableRowSorter<>(model);
        catalogTable.setRowSorter(catalogSorter);

        JButton reg = styledBtn("Register Selected");

        reg.addActionListener(e -> onRegister());

        JPanel actions = new JPanel(new BorderLayout());

        String dl = getRegistrationDeadline();
        JLabel deadlineLbl = new JLabel(
                (dl == null || dl.isBlank()) ?
                        "Registration deadline: Not set" :
                        "Registration deadline: " + dl
        );
        deadlineLbl.setForeground(Color.DARK_GRAY);
        deadlineLbl.setFont(deadlineLbl.getFont().deriveFont(Font.BOLD, 12f));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        left.setOpaque(false);
        left.add(reg);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        right.add(deadlineLbl);

        actions.add(left, BorderLayout.WEST);
        actions.add(right, BorderLayout.EAST);

        p.add(actions, BorderLayout.NORTH);
        p.add(new JScrollPane(catalogTable), BorderLayout.CENTER);
        return p;
    }

    private void refreshCatalog() {
        try {
            List<Map<String,Object>> list = service.getCourseCatalogForUI();
            DefaultTableModel m = (DefaultTableModel) catalogTable.getModel();
            m.setRowCount(0);

            for (Map<String,Object> r : list) {

                int instId = (int) r.get("instructor_id");
                String instructorName = getInstructorName(instId);

                m.addRow(new Object[]{
                        r.get("section_id"),
                        r.get("code"),
                        r.get("title"),
                        r.get("credits"),
                        r.get("capacity"),
                        instructorName
                });
            }
        } catch (Exception ignored) {}
    }


    private void onRegister() {
        int viewRow = catalogTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a section.");
            return;
        }
        String deadline = getRegistrationDeadline();
        if (deadline != null && !deadline.isBlank()) {
            try {
                LocalDateTime d = LocalDateTime.parse(deadline);
                if (LocalDateTime.now().isAfter(d)) {
                    JOptionPane.showMessageDialog(this,
                            "Course registration is closed. Deadline was: " + deadline);
                    return;
                }
            } catch (Exception ignored) {}
        }
        int modelRow = catalogTable.convertRowIndexToModel(viewRow);
        int sectionId = (int) catalogTable.getModel().getValueAt(modelRow, 0);

        JOptionPane.showMessageDialog(this, service.registerForSection(studentId, sectionId));
        refreshCatalog();
        refreshMyRegistrations();
    }
    private String getRegistrationDeadline() {
        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT setting_value FROM system_settings WHERE setting_key='registration_deadline'"
             )) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (Exception ignored) {}
        return null;
    }

    private JPanel buildMyRegPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(12,12,12,12));

        String[] cols = {"Section ID", "Code", "Title", "Credits"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        myRegTable = new JTable(model);
        myRegTable.setRowHeight(26);

        myRegSorter = new TableRowSorter<>(model);
        myRegTable.setRowSorter(myRegSorter);

        JButton drop = styledBtn("Drop Selected");
        drop.addActionListener(e -> onDrop());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8,6));
        actions.add(drop);

        p.add(actions, BorderLayout.NORTH);
        p.add(new JScrollPane(myRegTable), BorderLayout.CENTER);
        return p;
    }

    private void refreshMyRegistrations() {
        try {
            List<Map<String,Object>> list = service.getMyRegistrations(studentId);
            DefaultTableModel m = (DefaultTableModel) myRegTable.getModel();
            m.setRowCount(0);

            for (Map<String,Object> r : list) {
                m.addRow(new Object[]{
                        r.get("section_id"),
                        r.get("code"),
                        r.get("title"),
                        r.get("credits")
                });
            }
        } catch (Exception ignored) {}
    }

    private void onDrop() {
        int viewRow = myRegTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a row.");
            return;
        }
        int modelRow = myRegTable.convertRowIndexToModel(viewRow);
        int sectionId = (int) myRegTable.getModel().getValueAt(modelRow, 0);

        JOptionPane.showMessageDialog(this, service.dropSection(studentId, sectionId));
        refreshCatalog();
        refreshMyRegistrations();
    }

    private JPanel buildGradesPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(12,12,12,12));

        String[] cols = {"Code", "Title", "Credits", "Final Grade"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        gradesTable = new JTable(model);
        gradesTable.setRowHeight(26);

        gradesSorter = new TableRowSorter<>(model);
        gradesTable.setRowSorter(gradesSorter);

        JButton csv = styledBtn("Download Transcript (CSV)");
        csv.addActionListener(e -> exportCSV());
        JButton pdf = styledBtn("Download Transcript (PDF)");
        pdf.addActionListener(e -> exportPDF());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8,6));
        actions.add(csv);
        actions.add(pdf);

        p.add(actions, BorderLayout.NORTH);
        p.add(new JScrollPane(gradesTable), BorderLayout.CENTER);
        return p;
    }

    private void refreshGrades() {
        try {
            List<Map<String,Object>> list = service.getGrades(studentId);
            DefaultTableModel m = (DefaultTableModel) gradesTable.getModel();
            m.setRowCount(0);

            for (Map<String,Object> r : list) {
                m.addRow(new Object[]{
                        r.get("code"),
                        r.get("title"),
                        r.get("credits"),
                        r.get("grade")
                });
            }
        } catch (Exception ignored) {}
    }
    private String getInstructorName(int instructorId) {
        try (Connection con = DBConfig.getAuthDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT username FROM users_auth WHERE user_id = ?"
             )) {

            ps.setInt(1, instructorId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("username");  // correct column
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "N/A";
    }


    private void exportCSV() {
        try {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

            try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
                fw.write("Code,Title,Credits,Final Grade\n");
                for (int i = 0; i < gradesTable.getRowCount(); i++) {
                    fw.write(
                            gradesTable.getValueAt(i,0) + "," +
                                    gradesTable.getValueAt(i,1) + "," +
                                    gradesTable.getValueAt(i,2) + "," +
                                    gradesTable.getValueAt(i,3) + "\n"
                    );
                }
            }
            JOptionPane.showMessageDialog(this, "Transcript saved!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save.");
        }
    }
    private void exportPDF() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Transcript as PDF");
        fc.setFileFilter(new FileNameExtensionFilter("PDF Document", "pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File chosen = fc.getSelectedFile();
        String path = chosen.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) {
            path = path + ".pdf";
            chosen = new File(path);
        }

        PDDocument doc = new PDDocument();
        try {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            final float margin = 50;
            final float yStart = page.getMediaBox().getHeight() - margin;
            final float tableWidth = page.getMediaBox().getWidth() - 2*margin;
            final float rowHeight = 18;
            final float cellMargin = 5;

            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs.beginText();
            cs.newLineAtOffset(margin, yStart);
            cs.showText("Transcript");
            cs.endText();

            float y = yStart - 30;

            String[] headers = {"Code", "Title", "Credits", "Final Grade"};

            float[] colWidths = new float[] { 80, tableWidth - 80 - 60 - 80, 60, 80 };

            cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
            float x = margin;
            for (int i = 0; i < headers.length; i++) {
                cs.beginText();
                cs.newLineAtOffset(x + cellMargin, y);
                cs.showText(headers[i]);
                cs.endText();
                x += colWidths[i];
            }

            y -= rowHeight;

            cs.setFont(PDType1Font.HELVETICA, 10);
            int rows = gradesTable.getRowCount();
            for (int r = 0; r < rows; r++) {
                if (y < margin + rowHeight) {
                    cs.close();
                    page = new PDPage(PDRectangle.LETTER);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = page.getMediaBox().getHeight() - margin;
                }

                x = margin;
                Object code = gradesTable.getValueAt(r, 0);
                Object title = gradesTable.getValueAt(r, 1);
                Object credits = gradesTable.getValueAt(r, 2);
                Object finalGrade = gradesTable.getValueAt(r, 3);

                String[] cells = new String[] {
                        code == null ? "" : code.toString(),
                        title == null ? "" : title.toString(),
                        credits == null ? "" : credits.toString(),
                        finalGrade == null ? "" : finalGrade.toString()
                };

                for (int c = 0; c < cells.length; c++) {
                    String text = cells[c];
                    if (text.length() > 120) text = text.substring(0, 117) + "...";
                    cs.beginText();
                    cs.newLineAtOffset(x + cellMargin, y);
                    cs.showText(text);
                    cs.endText();
                    x += colWidths[c];
                }
                y -= rowHeight;
            }

            cs.close();

            doc.save(chosen);
            JOptionPane.showMessageDialog(this, "PDF saved: " + chosen.getAbsolutePath());

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to create PDF: " + ex.getMessage());
        } finally {
            try { doc.close(); } catch (IOException ignored) {}
        }
    }

    private void showProfilePopup(Component inv) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem pass = new JMenuItem("Change Password");
        JMenuItem logout = new JMenuItem("Logout");

        pass.addActionListener(e -> changePassword());
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        menu.add(pass);
        menu.add(logout);
        menu.show(inv, 0, inv.getHeight());
    }

    private void changePassword() {
        JPasswordField oldP = new JPasswordField();
        JPasswordField newP = new JPasswordField();
        JPasswordField confP = new JPasswordField();

        Object[] msg = {"Old Password:", oldP, "New Password:", newP, "Confirm New:", confP};
        if (JOptionPane.showConfirmDialog(this, msg, "Change Password", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        String oldS = new String(oldP.getPassword());
        String newS = new String(newP.getPassword());
        String confS = new String(confP.getPassword());

        if (!newS.equals(confS)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.");
            return;
        }
        if (newS.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password too short.");
            return;
        }

        try (Connection con = DBConfig.getAuthDataSource().getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                    "SELECT password_hash FROM users_auth WHERE user_id=?");
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            if (!PasswordHash.verify(oldS, rs.getString(1))) {
                JOptionPane.showMessageDialog(this, "Old password incorrect.");
                return;
            }

            PreparedStatement up = con.prepareStatement(
                    "UPDATE users_auth SET password_hash=? WHERE user_id=?");
            up.setString(1, PasswordHash.hash(newS));
            up.setInt(2, studentId);
            up.executeUpdate();

            JOptionPane.showMessageDialog(this, "Password updated!");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private JButton styledBtn(String t) {
        JButton b = new JButton(t);
        b.setBackground(ACTION_BG);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(ACTION_BG.darker()); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(ACTION_BG); }
        });
        return b;
    }
}

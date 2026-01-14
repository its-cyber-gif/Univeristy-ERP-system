package edu.univ.erp.data;
import edu.univ.erp.config.DBConfig;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NotificationDAO {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy • hh:mm a");  // e.g. 23 Nov 2025 • 12:49 PM

    public void add(int studentId, String msg) {
        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO notifications (student_id, message) VALUES (?, ?)"
             )) {
            ps.setInt(1, studentId);
            ps.setString(2, msg);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    public List<Map<String, Object>> listUnread(int studentId) {
        List<Map<String, Object>> out = new ArrayList<>();

        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT notification_id, message, created_at " +
                             "FROM notifications WHERE student_id = ? " +
                             "ORDER BY created_at DESC"
             )) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("notification_id"));
                m.put("msg", rs.getString("message"));
                m.put("time", rs.getTimestamp("created_at"));   // 👉 store the original Timestamp object

                out.add(m);
            }
        } catch (Exception ignored) {}

        return out;
    }
}

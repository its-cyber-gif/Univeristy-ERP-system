package edu.univ.erp.data;
import edu.univ.erp.config.DBConfig;
import edu.univ.erp.domain.Grade;
import java.sql.*;
import java.util.*;

public class GradeDAO {
    public List<Map<String, Object>> getGrades(int studentId) {
        List<Map<String, Object>> out = new ArrayList<>();

        String sql = """
        SELECT DISTINCT
            c.code,
            c.title,
            c.credits,
            g.final_grade
        FROM enrollments e
        JOIN sections s  ON e.section_id = s.section_id
        JOIN courses c   ON s.course_id  = c.course_id
        JOIN grades g    ON e.enrollment_id = g.enrollment_id
        WHERE e.student_id = ?
          AND g.final_grade IS NOT NULL
    """;

        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("code", rs.getString("code"));
                row.put("title", rs.getString("title"));
                row.put("credits", rs.getInt("credits"));
                row.put("grade", rs.getString("final_grade"));
                out.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }


    public boolean save(int enrollmentId, String component, Double score) {
        String sel = "SELECT grade_id FROM grades WHERE enrollment_id = ? AND component = ?";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sel)) {
            ps.setInt(1, enrollmentId);
            ps.setString(2, component);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                String upd = "UPDATE grades SET score = ? WHERE grade_id = ?";
                try (PreparedStatement ps2 = c.prepareStatement(upd)) {
                    ps2.setDouble(1, score);
                    ps2.setInt(2, id);
                    ps2.executeUpdate();
                    return true;
                }
            } else {
                String ins = "INSERT INTO grades (enrollment_id, component, score) VALUES (?,?,?)";
                try (PreparedStatement ps2 = c.prepareStatement(ins)) {
                    ps2.setInt(1, enrollmentId);
                    ps2.setString(2, component);
                    ps2.setDouble(3, score);
                    ps2.executeUpdate();
                    return true;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public int getTotalCreditsCompleted(int sid) {
        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT SUM(c.credits) " +
                             "FROM grades g JOIN enrollments e ON g.enrollment_id=e.enrollment_id " +
                             "JOIN sections s ON e.section_id=s.section_id " +
                             "JOIN courses c ON s.course_id=c.course_id " +
                             "WHERE e.student_id=? AND g.final_grade >= 40"
             )) {
            ps.setInt(1, sid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    public List<Grade> findByEnrollment(int enrollmentId) {
        List<Grade> out = new ArrayList<>();
        String sql = "SELECT grade_id, enrollment_id, component, score, final_grade FROM grades WHERE enrollment_id = ?";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Grade g = new Grade();
                g.setGradeId(rs.getInt("grade_id"));
                g.setEnrollmentId(rs.getInt("enrollment_id"));
                g.setComponent(rs.getString("component"));
                g.setScore(rs.getDouble("score"));
                if (rs.wasNull()) g.setScore(null);
                g.setFinalGrade(rs.getString("final_grade"));
                out.add(g);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    public boolean setFinalGrade(int enrollmentId, String finalGrade) {
        String check = "SELECT grade_id FROM grades WHERE enrollment_id = ?";
        String update = "UPDATE grades SET final_grade = ? WHERE enrollment_id = ?";
        String insert = "INSERT INTO grades (enrollment_id, final_grade) VALUES (?, ?)";

        try (Connection c = DBConfig.getErpDataSource().getConnection()) {
            try (PreparedStatement ps1 = c.prepareStatement(check)) {
                ps1.setInt(1, enrollmentId);
                ResultSet rs = ps1.executeQuery();
                if (rs.next()) {
                    try (PreparedStatement ps2 = c.prepareStatement(update)) {
                        ps2.setString(1, finalGrade);
                        ps2.setInt(2, enrollmentId);
                        ps2.executeUpdate();
                        return true;
                    }
                } else {
                    try (PreparedStatement ps3 = c.prepareStatement(insert)) {
                        ps3.setInt(1, enrollmentId);
                        ps3.setString(2, finalGrade);
                        ps3.executeUpdate();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}


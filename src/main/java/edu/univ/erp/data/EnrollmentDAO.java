package edu.univ.erp.data;
import edu.univ.erp.config.DBConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDAO {
    public boolean exists(int studentId, int sectionId) {
        String sql = "SELECT 1 FROM enrollments WHERE student_id = ? AND section_id = ? AND status = 'ENROLLED'";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean create(int studentId, int sectionId) {
        String updateSql =
                "UPDATE enrollments SET status='ENROLLED', enrolled_at=NOW() " +
                        "WHERE student_id = ? AND section_id = ?";

        String insertSql =
                "INSERT INTO enrollments (student_id, section_id, status) VALUES (?, ?, 'ENROLLED')";

        try (Connection c = DBConfig.getErpDataSource().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                ps.setInt(1, studentId);
                ps.setInt(2, sectionId);
                int rows = ps.executeUpdate();
                if (rows > 0) return true;
            }
            try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                ps.setInt(1, studentId);
                ps.setInt(2, sectionId);
                ps.executeUpdate();
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    public boolean drop(int studentId, int sectionId) {
        String sql = "UPDATE enrollments SET status = 'DROPPED' WHERE student_id = ? AND section_id = ? AND status = 'ENROLLED'";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            int changed = ps.executeUpdate();
            return changed > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public List<Integer> listSectionIdsForStudent(int studentId) {
        List<Integer> out = new ArrayList<>();
        String sql = "SELECT section_id FROM enrollments WHERE student_id = ? AND status = 'ENROLLED'";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getInt(1));
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    public List<Integer> listStudentIdsForSection(int sectionId) {
        List<Integer> out = new ArrayList<>();
        String sql = "SELECT student_id FROM enrollments WHERE section_id = ? AND status = 'ENROLLED'";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getInt(1));
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }
    public Integer getEnrollmentId(int studentId, int sectionId) {
        String sql = "SELECT enrollment_id FROM enrollments WHERE student_id = ? AND section_id = ?";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("enrollment_id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

package edu.univ.erp.data;
import edu.univ.erp.config.DBConfig;
import edu.univ.erp.domain.Course;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {
    public List<Course> listAll() {
        List<Course> out = new ArrayList<>();
        String sql = "SELECT course_id, code, title, credits FROM courses ORDER BY code";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Course cc = new Course();
                cc.setCourseId(rs.getInt("course_id"));
                cc.setCode(rs.getString("code"));
                cc.setTitle(rs.getString("title"));
                cc.setCredits(rs.getInt("credits"));
                out.add(cc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public int create(Course course) {
        String sql = "INSERT INTO courses (code, title, credits) VALUES (?,?,?)";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, course.getCode());
            ps.setString(2, course.getTitle());
            ps.setInt(3, course.getCredits());
            ps.executeUpdate();
            var rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }
    public Course findById(int courseId) {
        String sql = "SELECT course_id, code, title, credits FROM courses WHERE course_id = ?";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Course cc = new Course();
                cc.setCourseId(rs.getInt("course_id"));
                cc.setCode(rs.getString("code"));
                cc.setTitle(rs.getString("title"));
                cc.setCredits(rs.getInt("credits"));
                return cc;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public boolean update(int id, String code, String title, int credits) {
        String sql = "UPDATE courses SET code=?, title=?, credits=? WHERE course_id=?";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, title);
            ps.setInt(3, credits);
            ps.setInt(4, id);
            return ps.executeUpdate() == 1;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

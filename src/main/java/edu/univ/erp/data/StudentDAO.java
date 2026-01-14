package edu.univ.erp.data;
import edu.univ.erp.config.DBConfig;
import edu.univ.erp.domain.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentDAO {
    public Student findByUserId(int userId) {
        String sql = "SELECT user_id, roll_no, program, year FROM students WHERE user_id = ?";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Student s = new Student();
                s.setUserId(rs.getInt("user_id"));
                s.setRollNo(rs.getString("roll_no"));
                s.setProgram(rs.getString("program"));
                s.setYear(rs.getInt("year"));
                return s;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public int create(Student s) {
        String sql = "INSERT INTO students (user_id, roll_no, program, year) VALUES (?,?,?,?)";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, s.getUserId());
            ps.setString(2, s.getRollNo());
            ps.setString(3, s.getProgram());
            ps.setInt(4, s.getYear());
            return ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }
}



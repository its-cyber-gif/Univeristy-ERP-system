package edu.univ.erp.data;
import edu.univ.erp.config.DBConfig;
import edu.univ.erp.domain.Section;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SectionDAO {
    public List<Section> listAll() {
        List<Section> out = new ArrayList<>();
        String sql = "SELECT * FROM sections";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rowToSection(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    public Section findById(int id) {
        String sql = "SELECT * FROM sections WHERE section_id = ?";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rowToSection(rs);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public int countEnrolled(int sectionId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE section_id = ? AND status = 'ENROLLED'";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public int create(Section s) {
        String sql = "INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getCourseId());
            if (s.getInstructorId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, s.getInstructorId());
            ps.setString(3, s.getDayTime());
            ps.setString(4, s.getRoom());
            ps.setInt(5, s.getCapacity());
            ps.setString(6, s.getSemester());
            ps.setInt(7, s.getYear());
            ps.executeUpdate();
            var rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    private Section rowToSection(ResultSet rs) throws SQLException {
        Section s = new Section();
        s.setSectionId(rs.getInt("section_id"));
        s.setCourseId(rs.getInt("course_id"));
        int instr = rs.getInt("instructor_id");
        if (rs.wasNull()) s.setInstructorId(null); else s.setInstructorId(instr);
        s.setDayTime(rs.getString("day_time"));
        s.setRoom(rs.getString("room"));
        s.setCapacity(rs.getInt("capacity"));
        s.setSemester(rs.getString("semester"));
        s.setYear(rs.getInt("year"));
        return s;
    }
    public boolean update(Section s) {
        String sql = "UPDATE sections SET course_id=?, instructor_id=?, day_time=?, room=?, capacity=?, semester=?, year=? WHERE section_id=?";

        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, s.getCourseId());
            if (s.getInstructorId() == null)
                ps.setNull(2, java.sql.Types.INTEGER);
            else
                ps.setInt(2, s.getInstructorId());
            ps.setString(3, s.getDayTime());
            ps.setString(4, s.getRoom());
            ps.setInt(5, s.getCapacity());
            ps.setString(6, s.getSemester());
            ps.setInt(7, s.getYear());
            ps.setInt(8, s.getSectionId());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}


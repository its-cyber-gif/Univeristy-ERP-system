package edu.univ.erp.test;

import edu.univ.erp.config.DBConfig;
import edu.univ.erp.data.GradeDAO;
import org.junit.jupiter.api.*;

import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

public class GradeDaoAndCgpaTest {

    static int studentId;
    static int enrollmentId;

    @BeforeAll
    static void setup() throws Exception {

        try (Connection auth = DBConfig.getAuthDataSource().getConnection();
             Connection erp = DBConfig.getErpDataSource().getConnection()) {

            PreparedStatement ps = auth.prepareStatement("INSERT INTO users_auth (username, role, password_hash, status) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, "grade_test_user");
            ps.setString(2, "STUDENT");
            ps.setString(3, "x");
            ps.setString(4, "ACTIVE");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            studentId = rs.getInt(1);

            PreparedStatement psS = erp.prepareStatement("INSERT INTO students (user_id, roll_no, program, year) VALUES (?,?,?,?)");
            psS.setInt(1, studentId);
            psS.setString(2, "GR100");
            psS.setString(3, "TEST");
            psS.setInt(4, 1);
            psS.executeUpdate();

            PreparedStatement pc = erp.prepareStatement("INSERT INTO courses (code, title, credits) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
            pc.setString(1, "GR101");
            pc.setString(2, "Grade Course");
            pc.setInt(3, 4);
            pc.executeUpdate();
            ResultSet rc = pc.getGeneratedKeys();
            rc.next();
            int courseId = rc.getInt(1);

            PreparedStatement psSec = erp.prepareStatement("INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            psSec.setInt(1, courseId);
            psSec.setNull(2, Types.INTEGER);
            psSec.setString(3, "Tue 9-10");
            psSec.setString(4, "R2");
            psSec.setInt(5, 50);
            psSec.setString(6, "Fall");
            psSec.setInt(7, 2025);
            psSec.executeUpdate();
            ResultSet rsS = psSec.getGeneratedKeys();
            rsS.next();
            int sectionId = rsS.getInt(1);

            PreparedStatement pen = erp.prepareStatement("INSERT INTO enrollments (student_id, section_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            pen.setInt(1, studentId);
            pen.setInt(2, sectionId);
            pen.executeUpdate();
            ResultSet ren = pen.getGeneratedKeys();
            ren.next();
            enrollmentId = ren.getInt(1);
        }
    }

    @Test
    public void testFinalGradeComputeAndStore() throws Exception {
        GradeDAO gradeDAO = new GradeDAO();
        assertTrue(gradeDAO.save(enrollmentId, "quiz", 10.0));
        assertTrue(gradeDAO.save(enrollmentId, "midterm", 20.0));
        assertTrue(gradeDAO.save(enrollmentId, "endsem", 60.0));
        assertTrue(gradeDAO.save(enrollmentId, "assignment", 5.0));
        assertTrue(gradeDAO.save(enrollmentId, "final", 95.0));
        gradeDAO.setFinalGrade(enrollmentId, "95");
        assertTrue(gradeDAO.findByEnrollment(enrollmentId).stream().anyMatch(g -> "final".equalsIgnoreCase(g.getComponent())));
    }

    @AfterAll
    static void cleanup() throws Exception {
        try (Connection erp = DBConfig.getErpDataSource().getConnection();
             Connection auth = DBConfig.getAuthDataSource().getConnection()) {

            erp.prepareStatement("DELETE FROM grades WHERE enrollment_id = " + enrollmentId).executeUpdate();
            erp.prepareStatement("DELETE FROM enrollments WHERE enrollment_id = " + enrollmentId).executeUpdate();
            erp.prepareStatement("DELETE c FROM courses c WHERE c.code='GR101'").executeUpdate();
            erp.prepareStatement("DELETE FROM students WHERE user_id = " + studentId).executeUpdate();
            auth.prepareStatement("DELETE FROM users_auth WHERE user_id = " + studentId).executeUpdate();
        }
    }
}


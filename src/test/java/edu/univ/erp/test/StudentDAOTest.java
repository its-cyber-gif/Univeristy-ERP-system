package edu.univ.erp.test;
import edu.univ.erp.config.DBConfig;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Student;
import org.junit.jupiter.api.*;

import java.sql.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StudentDAOTest {

    private static int testUserId;

    @BeforeAll
    static void setup() throws Exception {
        try (Connection auth = DBConfig.getAuthDataSource().getConnection()) {
            PreparedStatement ps = auth.prepareStatement(
                    "INSERT INTO users_auth (username, role, password_hash, status) VALUES ('stTest','STUDENT','dummy','ACTIVE')",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            testUserId = rs.getInt(1);
        }

        try (Connection erp = DBConfig.getErpDataSource().getConnection()) {
            PreparedStatement ps = erp.prepareStatement(
                    "INSERT INTO students (user_id, roll_no, program, year) VALUES (?,?,?,?)"
            );
            ps.setInt(1, testUserId);
            ps.setString(2, "22CS300");
            ps.setString(3, "CSE");
            ps.setInt(4, 2);
            ps.executeUpdate();
        }
    }

    @Test
    @Order(1)
    void testFindStudent() {
        StudentDAO dao = new StudentDAO();
        Student s = dao.findByUserId(testUserId);

        Assertions.assertNotNull(s);
        Assertions.assertEquals("22CS300", s.getRollNo());
        Assertions.assertEquals("CSE", s.getProgram());
    }

    @AfterAll
    static void cleanup() throws Exception {
        try (Connection erp = DBConfig.getErpDataSource().getConnection()) {
            erp.prepareStatement("DELETE FROM students WHERE user_id=" + testUserId).executeUpdate();
        }
        try (Connection auth = DBConfig.getAuthDataSource().getConnection()) {
            auth.prepareStatement("DELETE FROM users_auth WHERE user_id=" + testUserId).executeUpdate();
        }
    }
}

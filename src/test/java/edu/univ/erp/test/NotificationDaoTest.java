package edu.univ.erp.test;

import edu.univ.erp.data.NotificationDAO;
import edu.univ.erp.config.DBConfig;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationDaoTest {

    static int studentId;

    @BeforeAll
    static void setup() throws Exception {
        try (Connection auth = DBConfig.getAuthDataSource().getConnection();
             Connection erp = DBConfig.getErpDataSource().getConnection()) {

            PreparedStatement ps = auth.prepareStatement("INSERT INTO users_auth(username, role, password_hash, status) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, "notify_user");
            ps.setString(2, "STUDENT");
            ps.setString(3, "x");
            ps.setString(4, "ACTIVE");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys(); rs.next();
            studentId = rs.getInt(1);

            PreparedStatement psStu = erp.prepareStatement("INSERT INTO students (user_id, roll_no, program, year) VALUES (?,?,?,?)");
            psStu.setInt(1, studentId);
            psStu.setString(2, "NT100");
            psStu.setString(3, "TEST");
            psStu.setInt(4, 1);
            psStu.executeUpdate();
        }
    }

    @Test
    public void testAddAndListNotification() {
        NotificationDAO dao = new NotificationDAO();
        dao.add(studentId, "Unit Test Notification");

        List<Map<String,Object>> list = dao.listUnread(studentId);
        assertFalse(list.isEmpty());
        Map<String,Object> first = list.get(0);
        assertTrue(((String) first.get("msg")).contains("Unit Test Notification"));
        assertNotNull(first.get("time"));
    }

    @AfterAll
    static void cleanup() throws Exception {
        try (Connection erp = DBConfig.getErpDataSource().getConnection();
             Connection auth = DBConfig.getAuthDataSource().getConnection()) {

            erp.prepareStatement("DELETE FROM notifications WHERE student_id = " + studentId).executeUpdate();
            erp.prepareStatement("DELETE FROM students WHERE user_id = " + studentId).executeUpdate();
            auth.prepareStatement("DELETE FROM users_auth WHERE user_id = " + studentId).executeUpdate();
        }
    }
}

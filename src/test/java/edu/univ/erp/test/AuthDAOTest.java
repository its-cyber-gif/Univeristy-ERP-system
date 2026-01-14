package edu.univ.erp.test;

import edu.univ.erp.config.DBConfig;
import org.junit.jupiter.api.*;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class AuthDAOTest {

    private static int testUserId;

    @BeforeAll
    static void setupUser() throws Exception {
        try (Connection c = DBConfig.getAuthDataSource().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users_auth (username, role, password_hash, status) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, "test_user_junit");
            ps.setString(2, "STUDENT");
            ps.setString(3, "$2a$10$invalidhashforsample"); // not used
            ps.setString(4, "ACTIVE");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            testUserId = rs.getInt(1);
        }
    }

    @Test
    public void testUserExistsInAuth() throws Exception {
        try (Connection c = DBConfig.getAuthDataSource().getConnection()) {
            PreparedStatement ps = c.prepareStatement("SELECT username, role FROM users_auth WHERE user_id=?");
            ps.setInt(1, testUserId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("test_user_junit", rs.getString("username"));
            assertEquals("STUDENT", rs.getString("role"));
        }
    }

    @AfterAll
    static void cleanupUser() throws Exception {
        try (Connection c = DBConfig.getAuthDataSource().getConnection()) {
            PreparedStatement del = c.prepareStatement("DELETE FROM users_auth WHERE user_id=?");
            del.setInt(1, testUserId);
            del.executeUpdate();
        }
    }
}

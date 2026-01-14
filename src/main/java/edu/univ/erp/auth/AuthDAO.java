package edu.univ.erp.auth;
import edu.univ.erp.config.DBConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class AuthDAO {
    public static class AuthRecord {
        public int userId;
        public String username;
        public String role;
        public String passwordHash;
    }

    public Optional<AuthRecord> findByUsername(String username) {
        String sql = "SELECT user_id, username, role, password_hash FROM users_auth WHERE username = ?";
        try (Connection c = DBConfig.getAuthDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                AuthRecord r = new AuthRecord();
                r.userId = rs.getInt("user_id");
                r.username = rs.getString("username");
                r.role = rs.getString("role");
                r.passwordHash = rs.getString("password_hash");
                return Optional.of(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}



package edu.univ.erp.auth;
import edu.univ.erp.config.DBConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Optional;

public class AuthService {
    private AuthDAO dao = new AuthDAO();
    public static class AuthResult {
        public boolean success;
        public String message;
        public int userId;
        public String role;
    }
    public AuthResult login(String username, String password) {
        AuthResult out = new AuthResult();
        Optional<AuthDAO.AuthRecord> recOpt = dao.findByUsername(username);
        if (recOpt.isEmpty()) {
            out.success = false;
            out.message = "Incorrect username or password.";
            return out;
        }
        AuthDAO.AuthRecord rec = recOpt.get();
        if (!PasswordHash.verify(password, rec.passwordHash)) {
            out.success = false;
            out.message = "Incorrect username or password.";
            recordAttempt(rec.userId, false);
            return out;
        }
        out.success = true;
        out.message = "Login successful.";
        out.userId = rec.userId;
        out.role = rec.role;
        recordAttempt(rec.userId, true);
        setLastLogin(rec.userId);
        return out;
    }

    private void recordAttempt(int userId, boolean successful) {
        try (Connection c = DBConfig.getAuthDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO login_attempts (user_id, successful) VALUES (?, ?)")) {
            ps.setInt(1, userId);
            ps.setBoolean(2, successful);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setLastLogin(int userId) {
        try (Connection c = DBConfig.getAuthDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE users_auth SET last_login = ? WHERE user_id = ?")) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

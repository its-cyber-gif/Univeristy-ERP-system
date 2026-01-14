package edu.univ.erp.access;
import edu.univ.erp.config.DBConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AccessControl {
    public static boolean isMaintenanceOn() {
        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT value FROM settings WHERE `key` = 'maintenance_on'")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "true".equalsIgnoreCase(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void setMaintenance(boolean flag) {
        try (Connection con = DBConfig.getAuthDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE system_settings SET setting_value=? WHERE setting_key='maintenance'"
            );
            ps.setString(1, flag ? "ON" : "OFF");
            ps.executeUpdate();
        } catch (Exception ignored) { }
    }

    public static void toggleMaintenance() {
        boolean cur = isMaintenanceOn();
        boolean next = !cur;

        try (Connection c = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE settings SET value = ? WHERE `key` = 'maintenance_on'"
             )) {

            ps.setString(1, next ? "true" : "false");
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String getRegistrationDeadline() {
        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT setting_value FROM system_settings WHERE setting_key='registration_deadline'"
             )) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (Exception ignored) {}
        return null;
    }

    public static void setRegistrationDeadline(String deadline) {
        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE system_settings SET setting_value=? WHERE setting_key='registration_deadline'"
             )) {
            ps.setString(1, deadline);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}

package edu.univ.erp.config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.Properties;
import java.io.InputStream;

public class DBConfig {
    private static HikariDataSource authDs;
    private static HikariDataSource erpDs;

    static {
        try (InputStream in = DBConfig.class.getResourceAsStream("/application.properties")) {
            Properties p = new Properties();
            p.load(in);

            HikariConfig authCfg = new HikariConfig();
            authCfg.setJdbcUrl(p.getProperty("auth.jdbc"));
            authCfg.setUsername(p.getProperty("auth.username"));
            authCfg.setPassword(p.getProperty("auth.password"));
            authCfg.setDriverClassName(p.getProperty("auth.driver"));

            HikariConfig erpCfg = new HikariConfig();
            erpCfg.setJdbcUrl(p.getProperty("erp.jdbc"));
            erpCfg.setUsername(p.getProperty("erp.username"));
            erpCfg.setPassword(p.getProperty("erp.password"));
            erpCfg.setDriverClassName(p.getProperty("erp.driver"));

            authDs = new HikariDataSource(authCfg);
            erpDs = new HikariDataSource(erpCfg);

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static DataSource getAuthDataSource() { return authDs; }
    public static DataSource getErpDataSource() { return erpDs; }
}


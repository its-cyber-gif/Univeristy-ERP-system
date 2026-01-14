package edu.univ.erp.test;

import edu.univ.erp.config.DBConfig;
import edu.univ.erp.access.AccessControl;
import org.junit.jupiter.api.*;

import java.sql.*;

public class SystemSettingsTest {

    @Test
    void testToggleMaintenance() throws Exception {
        boolean before = AccessControl.isMaintenanceOn();

        AccessControl.toggleMaintenance();
        boolean after = AccessControl.isMaintenanceOn();

        Assertions.assertNotEquals(before, after);

        AccessControl.toggleMaintenance();
    }
}


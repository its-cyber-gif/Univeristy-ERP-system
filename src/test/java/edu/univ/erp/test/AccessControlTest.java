package edu.univ.erp.test;

import edu.univ.erp.access.AccessControl;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccessControlTest {

    @BeforeAll
    static void beforeAll() {
        AccessControl.setMaintenance(false);
        AccessControl.setRegistrationDeadline(null);
    }

    @Test
    @Order(1)
    public void testToggleMaintenance() {
        AccessControl.toggleMaintenance();
        assertTrue(AccessControl.isMaintenanceOn());
        AccessControl.toggleMaintenance();
        assertFalse(AccessControl.isMaintenanceOn());
    }

    @Test
    @Order(2)
    public void testRegistrationDeadline() {
        String dt = LocalDateTime.now().plusDays(1).toString();
        AccessControl.setRegistrationDeadline(dt);
        assertEquals(dt, AccessControl.getRegistrationDeadline());
        AccessControl.setRegistrationDeadline(null);
        assertNull(AccessControl.getRegistrationDeadline());
    }
}


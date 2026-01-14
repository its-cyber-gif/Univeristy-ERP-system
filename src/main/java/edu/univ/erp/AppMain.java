package edu.univ.erp;
import com.formdev.flatlaf.FlatLightLaf;
import edu.univ.erp.ui.auth.LoginFrame;
import javax.swing.*;

public class AppMain {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("FlatLaf not available, using default.");
        }

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });

    }
}


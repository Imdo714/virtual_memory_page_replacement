package com.page.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class NURSwingApplication {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setLookAndFeel();
            NURSwingFrame frame = new NURSwingFrame();
            frame.setVisible(true);
        });
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 기본 LookAndFeel을 사용한다.
        }
    }
}

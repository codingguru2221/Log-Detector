package com.darkeye.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Lightweight Swing fallback admin UI for environments where JavaFX is not available.
 */
public class SwingAdminApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwingAdminApp::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("DarkEye - Admin (Swing)");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("🔍 DarkEye Dashboard - ADMIN", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        root.add(title, BorderLayout.NORTH);

        // Center area: logs / alerts preview
        JTextArea centerArea = new JTextArea();
        centerArea.setEditable(false);
        centerArea.setText("Status: Monitoring Active\nLogs Processed: N/A\nAlerts Generated: N/A\n\n" +
                "This is a lightweight Swing fallback UI. For full functionality, run the JavaFX UI when available.");
        centerArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(centerArea);
        root.add(scroll, BorderLayout.CENTER);

        // Control panel
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton startBtn = new JButton("Start Monitoring");
        JButton stopBtn = new JButton("Stop Monitoring");
        JButton addIpBtn = new JButton("Add IP to Blacklist");
        JButton exitBtn = new JButton("Exit");

        startBtn.addActionListener(e -> JOptionPane.showMessageDialog(frame, "Start Monitoring clicked (no-op in fallback UI)", "Info", JOptionPane.INFORMATION_MESSAGE));
        stopBtn.addActionListener(e -> JOptionPane.showMessageDialog(frame, "Stop Monitoring clicked (no-op in fallback UI)", "Info", JOptionPane.INFORMATION_MESSAGE));
        addIpBtn.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog(frame, "Enter IP to blacklist:", "Add IP", JOptionPane.PLAIN_MESSAGE);
            if (ip != null && !ip.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "IP '" + ip.trim() + "' added to blacklist (no-op in fallback UI)", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        exitBtn.addActionListener(e -> frame.dispose());

        controls.add(startBtn);
        controls.add(stopBtn);
        controls.add(addIpBtn);
        controls.add(exitBtn);

        root.add(controls, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }
}

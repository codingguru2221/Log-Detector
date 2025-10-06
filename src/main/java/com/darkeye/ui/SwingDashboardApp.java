package com.darkeye.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// JavaFX embedding
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

/**
 * Rich Swing dashboard implementing requested layout:
 * - Top menu with Key options (Create / Recover)
 * - Left: Device list with id, type, colored status badge
 * - Center: Live events table (time, device, metric, value, score)
 * - Right-top: High-priority alert cards
 * - Right-bottom: Action buttons (Import Rules, Export Report, Quarantine)
 *
 * Includes demo data generator so the UI looks active for presentations.
 */
public class SwingDashboardApp {

    private JFrame frame;
    private DefaultListModel<Device> deviceListModel = new DefaultListModel<>();
    private JTable eventsTable;
    private DefaultTableModel eventsTableModel;
    private JPanel alertsPanel;
    private ScheduledExecutorService generator = Executors.newSingleThreadScheduledExecutor();
    private Random random = new Random();
    // JavaFX chart integration
    private JFXPanel fxChartPanel;
    private XYChart.Series<Number, Number> series;
    private volatile int chartX = 0;

    public static void main(String[] args) {
        // Try Nimbus L&F for a modern look
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new SwingDashboardApp().createAndShow());
    }

    private void createAndShow() {
        frame = new JFrame("DarkEye - Swing Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);

        frame.setLayout(new BorderLayout());
        frame.add(createMenuBar(), BorderLayout.NORTH);
        frame.add(createMainPanel(), BorderLayout.CENTER);

        // Footer status
        JLabel footer = new JLabel("Status: Ready");
        footer.setBorder(new EmptyBorder(6, 10, 6, 10));
        frame.add(footer, BorderLayout.SOUTH);

        // Populate demo devices
        populateDemoDevices();

        // Start demo live events
        startDemoGenerator();

    // initialize JavaFX chart panel after frame exists
    initFxChart();

        frame.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu keyMenu = new JMenu("Key");
        JMenuItem createKey = new JMenuItem(new AbstractAction("Create Key") {
            @Override
            public void actionPerformed(ActionEvent e) {
                createKeyDialog();
            }
        });
        JMenuItem recoverKey = new JMenuItem(new AbstractAction("Recover Key") {
            @Override
            public void actionPerformed(ActionEvent e) {
                recoverKeyDialog();
            }
        });
        keyMenu.add(createKey);
        keyMenu.add(recoverKey);
        menuBar.add(keyMenu);

        JMenu view = new JMenu("View");
        JMenuItem clearAlerts = new JMenuItem(new AbstractAction("Clear Alerts") {
            @Override
            public void actionPerformed(ActionEvent e) {
                alertsPanel.removeAll();
                alertsPanel.revalidate();
                alertsPanel.repaint();
            }
        });
        view.add(clearAlerts);
        menuBar.add(view);

        return menuBar;
    }

    private JSplitPane createMainPanel() {
        // Left: device list
        JList<Device> deviceJList = new JList<>(deviceListModel);
        deviceJList.setCellRenderer(new DeviceCellRenderer());
        deviceJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane leftScroll = new JScrollPane(deviceJList);
        leftScroll.setBorder(BorderFactory.createTitledBorder("Devices"));

        // Center: Live events table
        String[] cols = new String[]{"Time", "Device", "Metric", "Value", "Score"};
        eventsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        eventsTable = new JTable(eventsTableModel);
        eventsTable.setFillsViewportHeight(true);
        eventsTable.setRowHeight(26);
        JScrollPane centerScroll = new JScrollPane(eventsTable);
        centerScroll.setBorder(BorderFactory.createTitledBorder("Live Events"));

        // Right: top alerts and bottom actions
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));

        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        JScrollPane alertsScroll = new JScrollPane(alertsPanel);
        alertsScroll.setBorder(BorderFactory.createTitledBorder("High-Priority Alerts"));
        alertsScroll.setPreferredSize(new Dimension(320, 360));

        JPanel actionsPanel = new JPanel(new GridLayout(3, 1, 8, 8));
        actionsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        JButton importBtn = new JButton("Import Rules");
        JButton exportBtn = new JButton("Export Report");
        JButton quarantineBtn = new JButton("Quarantine");

        importBtn.addActionListener(e -> importRules());
        exportBtn.addActionListener(e -> exportReport());
        quarantineBtn.addActionListener(e -> quarantineSelectedDevice(deviceJList.getSelectedValue()));

        actionsPanel.add(importBtn);
        actionsPanel.add(exportBtn);
        actionsPanel.add(quarantineBtn);

    // JavaFX chart panel above actions
    fxChartPanel = new JFXPanel();
    fxChartPanel.setPreferredSize(new Dimension(320, 200));
    JPanel fxContainer = new JPanel(new BorderLayout());
    fxContainer.setBorder(BorderFactory.createTitledBorder("Live Metrics"));
    fxContainer.add(fxChartPanel, BorderLayout.CENTER);

    JPanel rightSouth = new JPanel(new BorderLayout(6,6));
    rightSouth.add(fxContainer, BorderLayout.CENTER);
    rightSouth.add(actionsPanel, BorderLayout.SOUTH);

    rightPanel.add(alertsScroll, BorderLayout.CENTER);
    rightPanel.add(rightSouth, BorderLayout.SOUTH);

        // Split center/right vertically
        JSplitPane centerRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerScroll, rightPanel);
        centerRight.setResizeWeight(0.7);

        // Main split: left and (center+right)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, centerRight);
        mainSplit.setResizeWeight(0.18);

        // Quick interactions: double-click device to show details
        deviceJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Device d = deviceJList.getSelectedValue();
                    if (d != null) showDeviceDetails(d);
                }
            }
        });

        // Table double-click shows event details
        eventsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = eventsTable.getSelectedRow();
                    if (r >= 0) showEventDetails(r);
                }
            }
        });

        return mainSplit;
    }

    private void populateDemoDevices() {
        deviceListModel.addElement(new Device("device-01", "Camera", Status.ONLINE));
        deviceListModel.addElement(new Device("device-02", "Sensor", Status.ONLINE));
        deviceListModel.addElement(new Device("device-03", "Server", Status.WARNING));
        deviceListModel.addElement(new Device("device-04", "Workstation", Status.OFFLINE));
        deviceListModel.addElement(new Device("device-05", "Router", Status.ONLINE));
    }

    private void startDemoGenerator() {
        Runnable producer = () -> {
            SwingUtilities.invokeLater(() -> {
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                Device d = pickRandomDevice();
                String metric = pickMetric();
                double value = 10 + random.nextDouble() * 90;
                double score = Math.min(100, Math.round((value / 100.0) * 100.0) / 1.0);
                DecimalFormat df = new DecimalFormat("0.00");
                eventsTableModel.insertRow(0, new Object[]{time, d.id + " (" + d.type + ")", metric, df.format(value), (int) score});

                // Keep table reasonable
                if (eventsTableModel.getRowCount() > 500) {
                    eventsTableModel.removeRow(eventsTableModel.getRowCount() - 1);
                }

                // Spawn alert card for high score
                if (score > 85) {
                    addAlertCard("High Score on " + d.id, "Metric " + metric + " reported score " + (int) score);
                    showNotification("High Priority Alert", d.id + ": " + metric + " -> " + (int) score, true);
                }
                // push to JavaFX live chart (metric value)
                pushChartPoint(value);
            });
        };

        generator.scheduleAtFixedRate(producer, 0, 1, TimeUnit.SECONDS);
    }

    private Device pickRandomDevice() {
        if (deviceListModel.isEmpty()) return new Device("unknown", "unknown", Status.OFFLINE);
        int i = random.nextInt(deviceListModel.size());
        return deviceListModel.get(i);
    }

    private String pickMetric() {
        String[] metrics = new String[]{"CPU", "Connections", "AuthFailures", "PacketLoss", "Latency"};
        return metrics[random.nextInt(metrics.length)];
    }

    private void addAlertCard(String title, String message) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(new Color(200, 40, 40), 2, true));
        card.setBackground(new Color(255, 245, 245));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 14f));
        JLabel m = new JLabel(message);
        m.setFont(m.getFont().deriveFont(12f));

        card.add(t, BorderLayout.NORTH);
        card.add(m, BorderLayout.CENTER);

        JButton ack = new JButton("Acknowledge");
        ack.addActionListener(e -> {
            alertsPanel.remove(card);
            alertsPanel.revalidate();
            alertsPanel.repaint();
        });
        card.add(ack, BorderLayout.EAST);

        alertsPanel.add(card, 0);
        alertsPanel.revalidate();
        alertsPanel.repaint();
    }

    private void showDeviceDetails(Device d) {
        JOptionPane.showMessageDialog(frame, String.format("Device ID: %s\nType: %s\nStatus: %s", d.id, d.type, d.status), "Device Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showEventDetails(int row) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < eventsTable.getColumnCount(); c++) {
            sb.append(eventsTable.getColumnName(c)).append(": ").append(eventsTable.getValueAt(row, c)).append("\n");
        }
        JOptionPane.showMessageDialog(frame, sb.toString(), "Event Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void createKeyDialog() {
        String key = JTextFieldEditor("Create Key", "Enter passphrase:");
        if (key != null && !key.trim().isEmpty()) {
            // In a real app we'd generate and store a key; here we mock
            showNotification("Key Created", "New key created successfully.", false);
            JOptionPane.showMessageDialog(frame, "Key created:\n" + key, "Create Key", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void recoverKeyDialog() {
        String email = JTextFieldEditor("Recover Key", "Enter recovery email:");
        if (email != null && !email.trim().isEmpty()) {
            showNotification("Key Recovery", "Recovery instructions sent to " + email, false);
            JOptionPane.showMessageDialog(frame, "Recovery instructions have been sent to " + email, "Recover Key", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String JTextFieldEditor(String title, String message) {
        JTextField field = new JTextField();
        Object[] msg = new Object[]{message, field};
        int res = JOptionPane.showConfirmDialog(frame, msg, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) return field.getText();
        return null;
    }

    private void importRules() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Rules File");
        int ret = chooser.showOpenDialog(frame);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            showNotification("Import Rules", "Imported: " + f.getName(), false);
        }
    }

    private void exportReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Report (save)");
        int ret = chooser.showSaveDialog(frame);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            showNotification("Export Report", "Saved: " + f.getName(), false);
        }
    }

    private void quarantineSelectedDevice(Device s) {
        if (s == null) {
            JOptionPane.showMessageDialog(frame, "Select a device to quarantine (double-click to open details)", "Quarantine", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int res = JOptionPane.showConfirmDialog(frame, "Quarantine device " + s.id + "?", "Quarantine", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            showNotification("Quarantine", "Device " + s.id + " quarantined", true);
            s.status = Status.OFFLINE;
            // force list repaint
            deviceListModel.set(deviceListModel.indexOf(s), s);
        }
    }

    /**
     * Show a notification. Tries SystemTray balloon if supported, else falls back to JOptionPane modal.
     */
    private void showNotification(String title, String message, boolean highPriority) {
        // Try SystemTray
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image img = createTrayImage();
                TrayIcon icon = new TrayIcon(img, "DarkEye");
                icon.setImageAutoSize(true);
                tray.add(icon);
                icon.displayMessage(title, message, highPriority ? TrayIcon.MessageType.ERROR : TrayIcon.MessageType.INFO);

                // Remove icon after a short delay so multiple messages don't pile up
                Executors.newSingleThreadScheduledExecutor().schedule(() -> tray.remove(icon), 5, TimeUnit.SECONDS);
                return;
            } catch (Exception ignored) {
            }
        }

        // Fallback: JOptionPane (modal)
        if (highPriority) {
            JOptionPane.showMessageDialog(frame, message, title, JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private Image createTrayImage() {
        try {
            // try to load an embedded resource icon if present
            URL u = getClass().getResource("/ui/icon.png");
            if (u != null) return ImageIO.read(u).getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        } catch (IOException ignored) {
        }
        // fallback: draw a small red/black circle
        Image img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 16, 16);
        g.setColor(Color.RED);
        g.fillOval(2, 2, 12, 12);
        g.dispose();
        return img;
    }

    // --- JavaFX chart helpers ---
    private void initFxChart() {
        // Ensure JavaFX platform is started
        Platform.runLater(() -> {
            try {
                NumberAxis xAxis = new NumberAxis();
                NumberAxis yAxis = new NumberAxis(0, 100, 10);
                xAxis.setLabel("Time");
                yAxis.setLabel("Metric Value");

                LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
                chart.setLegendVisible(false);
                chart.setAnimated(false);
                chart.setCreateSymbols(false);
                chart.setTitle("Live Metric (simulated)");

                series = new XYChart.Series<>();
                chart.getData().add(series);

                VBox root = new VBox(chart);
                root.setSpacing(4);
                Scene scene = new Scene(root, 320, 200);
                fxChartPanel.setScene(scene);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void pushChartPoint(double value) {
        // push point to FX thread; keep only last 60 points
        if (series == null) return;
        Platform.runLater(() -> {
            series.getData().add(new XYChart.Data<>(chartX++, value));
            if (series.getData().size() > 60) {
                series.getData().remove(0);
            }
        });
    }

    // --- Helper classes ---
    private static class Device {
        String id;
        String type;
        Status status;

        Device(String id, String type, Status status) {
            this.id = id;
            this.type = type;
            this.status = status;
        }

        @Override
        public String toString() {
            return id + " — " + type;
        }
    }

    private enum Status {ONLINE, OFFLINE, WARNING}

    private static class DeviceCellRenderer extends JPanel implements ListCellRenderer<Device> {
        private JLabel title = new JLabel();
        private JLabel status = new JLabel();

        DeviceCellRenderer() {
            setLayout(new BorderLayout(6, 6));
            add(title, BorderLayout.CENTER);
            add(status, BorderLayout.EAST);
            setBorder(new EmptyBorder(6, 6, 6, 6));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Device> list, Device value, int index, boolean isSelected, boolean cellHasFocus) {
            title.setText(value.id + " (" + value.type + ")");
            status.setText(" ");
            status.setOpaque(true);
            switch (value.status) {
                case ONLINE:
                    status.setBackground(new Color(60, 180, 75));
                    break;
                case WARNING:
                    status.setBackground(new Color(255, 165, 0));
                    break;
                case OFFLINE:
                default:
                    status.setBackground(new Color(200, 50, 50));
                    break;
            }
            status.setPreferredSize(new Dimension(14, 14));

            if (isSelected) setBackground(new Color(220, 235, 255));
            else setBackground(list.getBackground());

            return this;
        }
    }

}

package com.darkeye.ui;

import com.darkeye.model.SecurityAlert;
import com.darkeye.background.BackgroundDetectorService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * UIDashboard - shows alerts and allows role-based actions.
 */
public class UIDashboard {

    private final Stage stage;
    private final String role; // "viewer", "analyst", "admin"

    public UIDashboard(Stage stage, String role) {
        this.stage = stage;
        this.role = role;
    }

    public void show() {
        stage.setTitle("DarkEye Dashboard - Role: " + role);
        BorderPane root = new BorderPane();

        ListView<String> alertList = new ListView<>();
        Button refresh = new Button("Refresh Alerts");
        refresh.setOnAction(e -> {
            List<SecurityAlert> pending = BackgroundDetectorService.getInstance().drainPendingAlerts();
            for (SecurityAlert a : pending) {
                alertList.getItems().add(a.toString());
            }
        });

        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getChildren().addAll(new Label("Logged in as: " + role), refresh);

        root.setTop(topBar);

        VBox center = new VBox(10);
        center.setPadding(new Insets(10));
        center.getChildren().addAll(new Label("Recent Alerts:"), alertList);
        root.setCenter(center);

        if ("admin".equalsIgnoreCase(role)) {
            Button resolveBtn = new Button("Resolve Selected");
            resolveBtn.setOnAction(e -> {
                // placeholder: admin resolve logic
                String sel = alertList.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    System.out.println("[UIDashboard] Admin resolved: " + sel);
                }
            });
            root.setBottom(resolveBtn);
        }

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }
}

package com.darkeye.background;

import com.darkeye.model.SecurityAlert;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


/**
 * Small popup used by background service to prompt the user for actions on alerts.
 * Uses JavaFX to remain compatible with existing UI.
 */
public class AlertPopup {

    public static void showForAlert(SecurityAlert alert, BackgroundDetectorService service) {
        try {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UTILITY);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Security Alert");

            VBox content = new VBox(12);
            content.setPadding(new Insets(16));
            content.setAlignment(Pos.CENTER);

            Label title = new Label("🚨 " + alert.getTitle());
            Label desc = new Label(alert.getDescription());
            desc.setWrapText(true);

            Button ignoreBtn = new Button("Ignore");
            Button addDeviceBtn = new Button("Add as my device");
            Button resolveBtn = new Button("Resolve");

            ignoreBtn.setOnAction(e -> {
                service.handleIgnore(alert);
                stage.close();
            });

            addDeviceBtn.setOnAction(e -> {
                // call USBAuthManager via service helper
                service.handleAddAsDevice(alert, ok -> {
                    // simple UI feedback could be added
                    if (ok) stage.close();
                });
            });

            resolveBtn.setOnAction(e -> {
                service.handleResolve(alert, ok -> {
                    if (ok) stage.close();
                });
            });

            content.getChildren().addAll(title, desc, ignoreBtn, addDeviceBtn, resolveBtn);

            Scene scene = new Scene(content, 420, 220);
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            System.out.println("[AlertPopup] Unable to show popup: " + ex.getMessage());
        }
    }
}

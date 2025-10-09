package com.darkeye.ui;

import com.darkeye.auth.USBAuthManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Simplified LoginScreen that validates roles by checking for USB keys.
 */
public class LoginScreen {

    public interface LoginCallback {
        void onLogin(String role);
    }

    public static void show(Stage stage, LoginCallback cb) {
        stage.setTitle("DarkEye - Role Login");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label label = new Label("Insert USB key and choose role to login:");

        Button viewerBtn = new Button("Login as Viewer");
        viewerBtn.setOnAction(e -> {
            // viewers accepted if viewer key present
            boolean ok = USBAuthManager.requestUserKey();
            if (ok) cb.onLogin("viewer");
        });

        Button analystBtn = new Button("Login as Analyst");
        analystBtn.setOnAction(e -> {
            boolean ok = USBAuthManager.requestUserKey();
            if (ok) cb.onLogin("analyst");
        });

        Button adminBtn = new Button("Login as Admin");
        adminBtn.setOnAction(e -> {
            boolean ok = USBAuthManager.requestAdminKey();
            if (ok) cb.onLogin("admin");
        });

        root.getChildren().addAll(label, viewerBtn, analystBtn, adminBtn);

        Scene scene = new Scene(root, 360, 220);
        stage.setScene(scene);
        stage.show();
    }
}

package com.darkeye.ui;

import com.darkeye.background.BackgroundDetectorService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * MainAppLauncher - entry point that ensures background service is running
 * and opens the login/dashboard when requested.
 */
public class MainAppLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Start background detector (simulate auto-start)
        BackgroundDetectorService.simulateAutoStartOnBoot();

        // When user opens app, show login screen to choose role
        LoginScreen.show(primaryStage, role -> {
            Platform.runLater(() -> {
                UIDashboard dash = new UIDashboard(primaryStage, role);
                dash.show();
            });
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}

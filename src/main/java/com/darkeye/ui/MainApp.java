package com.darkeye.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for DarkEye log analysis system.
 * This is the entry point for the JavaFX application.
 */
public class MainApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("DarkEye starting...");
            
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/main.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            
            // Set up the stage
            primaryStage.setTitle("DarkEye - Log Analysis System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Show the stage
            primaryStage.show();
            
            logger.info("DarkEye UI loaded successfully");
            
        } catch (Exception e) {
            logger.error("Error starting DarkEye application", e);
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Main method - entry point for the application
     * @param args command line arguments
     */
    public static void main(String[] args) {
        logger.info("DarkEye application starting...");
        System.out.println("DarkEye starting");
        
        // Launch JavaFX application
        launch(args);
    }
}

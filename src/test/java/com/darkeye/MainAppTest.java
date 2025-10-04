package com.darkeye;

import com.darkeye.ui.MainApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MainApp class
 */
class MainAppTest {
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }
    
    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }
    
    @Test
    void testMainMethodRuns() {
        // Test that the main method can be called without throwing exceptions
        assertDoesNotThrow(() -> {
            // We can't actually run the full JavaFX application in unit tests
            // without additional setup, so we just verify the class loads
            MainApp app = new MainApp();
            assertNotNull(app);
        });
    }
    
    @Test
    void testMainAppInstantiation() {
        // Test that MainApp can be instantiated
        MainApp app = new MainApp();
        assertNotNull(app);
    }
}

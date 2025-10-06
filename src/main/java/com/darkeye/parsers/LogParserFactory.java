package com.darkeye.parsers;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class to create appropriate log parsers based on log format detection
 */
public class LogParserFactory {
    
    private static final List<LogParser> PARSERS = new ArrayList<>();
    
    static {
        // Register all available parsers
        PARSERS.add(new ApacheLogParser());
        PARSERS.add(new SyslogParser());
        PARSERS.add(new WindowsEventLogParser());
    }
    
    /**
     * Detect and return the appropriate parser for the given log format
     * @param sampleLines sample lines from the log file
     * @return the best matching parser or null if none found
     */
    public static LogParser detectParser(List<String> sampleLines) {
        if (sampleLines == null || sampleLines.isEmpty()) {
            return null;
        }
        
        // Try each parser with sample lines
        for (LogParser parser : PARSERS) {
            int matches = 0;
            for (String line : sampleLines) {
                if (parser.canParse(line)) {
                    matches++;
                }
            }
            
            // If more than 50% of sample lines match, use this parser
            if (matches > sampleLines.size() * 0.5) {
                return parser;
            }
        }
        
        return null;
    }
    
    /**
     * Get a specific parser by name
     * @param parserName the name of the parser
     * @return the parser or null if not found
     */
    public static LogParser getParser(String parserName) {
        return PARSERS.stream()
            .filter(p -> p.getParserName().equals(parserName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get all available parsers
     * @return list of all parsers
     */
    public static List<LogParser> getAllParsers() {
        return new ArrayList<>(PARSERS);
    }
    
    /**
     * Add a custom parser
     * @param parser the parser to add
     */
    public static void addParser(LogParser parser) {
        PARSERS.add(parser);
    }
}

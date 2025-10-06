package com.darkeye.parsers;

import com.darkeye.model.LogEntry;
import java.util.List;

/**
 * Base interface for all log parsers
 */
public interface LogParser {
    
    /**
     * Parse a single log line into a LogEntry
     * @param line the log line to parse
     * @param source the source file/path
     * @param lineNumber the line number in the source
     * @return parsed LogEntry or null if parsing fails
     */
    LogEntry parseLine(String line, String source, int lineNumber);
    
    /**
     * Check if this parser can handle the given log format
     * @param sampleLine a sample line from the log
     * @return true if this parser can handle the format
     */
    boolean canParse(String sampleLine);
    
    /**
     * Get the parser name/type
     * @return parser name
     */
    String getParserName();
}

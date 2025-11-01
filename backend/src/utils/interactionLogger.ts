import fs from "fs";
import path from "path";

// Define the structure of an interaction log entry
export interface InteractionLogEntry {
  timestamp: string;
  poster_id: string;
  script: string;
  feedback: "like" | "dislike" | null;
  response_time: number;
}

const LOG_FILE_PATH = path.join(__dirname, "../../logs/interaction_log.csv");

/**
 * Initialize the interaction log file with headers if it doesn't exist
 */
export function initializeInteractionLog(): void {
  try {
    // Create logs directory if it doesn't exist
    const logDir = path.dirname(LOG_FILE_PATH);
    if (!fs.existsSync(logDir)) {
      fs.mkdirSync(logDir, { recursive: true });
    }
    
    // Create log file with headers if it doesn't exist
    if (!fs.existsSync(LOG_FILE_PATH)) {
      const headers = "timestamp,poster_id,script,feedback,response_time\n";
      fs.writeFileSync(LOG_FILE_PATH, headers);
    }
  } catch (error) {
    console.error("Error initializing interaction log:", error);
  }
}

/**
 * Log an interaction entry
 * @param entry The interaction entry to log
 */
export function logInteraction(entry: InteractionLogEntry): void {
  try {
    // Format the entry as CSV
    const csvLine = `${entry.timestamp},${entry.poster_id},"${entry.script.replace(/"/g, '""')}",${entry.feedback || ""},${entry.response_time}\n`;
    
    // Append to log file
    fs.appendFileSync(LOG_FILE_PATH, csvLine);
  } catch (error) {
    console.error("Error logging interaction:", error);
  }
}

/**
 * Get recent interactions from the log
 * @param limit Number of recent interactions to retrieve (default: 100)
 * @returns Array of recent interaction entries
 */
export function getRecentInteractions(limit: number = 100): InteractionLogEntry[] {
  try {
    if (!fs.existsSync(LOG_FILE_PATH)) {
      return [];
    }
    
    const content = fs.readFileSync(LOG_FILE_PATH, "utf8");
    const lines = content.trim().split("\n");
    
    // Remove header line
    lines.shift();
    
    // Get recent entries (last 'limit' entries)
    const recentLines = lines.slice(-limit);
    
    // Parse CSV lines into objects
    const entries: InteractionLogEntry[] = [];
    for (const line of recentLines) {
      const parts = line.split(",");
      if (parts.length >= 5) {
        entries.push({
          timestamp: parts[0],
          poster_id: parts[1],
          script: parts[2].replace(/^"(.*)"$/, "$1").replace(/""/g, '"'), // Handle quoted strings
          feedback: parts[3] === "like" ? "like" : parts[3] === "dislike" ? "dislike" : null,
          response_time: parseInt(parts[4]) || 0
        });
      }
    }
    
    return entries.reverse(); // Return most recent first
  } catch (error) {
    console.error("Error reading recent interactions:", error);
    return [];
  }
}

// Initialize the log file when the module is loaded
initializeInteractionLog();
import fs from 'fs';
import path from 'path';

interface RequestLogEntry {
  timestamp: string;
  image_id: string;
  user_id?: string;
  script: string;
  language: string;
  tone: string;
  response_time: number;
  success: boolean;
  error?: string;
  metadata?: any;
}

interface AnalyticsData {
  total_requests: number;
  successful_requests: number;
  failed_requests: number;
  average_response_time: number;
  requests_by_poster: Record<string, number>;
  requests_by_language: Record<string, number>;
  requests_by_tone: Record<string, number>;
  requests_by_hour: Record<string, number>;
  last_updated: string;
}

export class DynamicScriptLogger {
  private logDir: string;
  private requestsLogFile: string;
  private analyticsFile: string;

  constructor() {
    this.logDir = path.join(__dirname, '../../logs');
    this.requestsLogFile = path.join(this.logDir, 'dynamic-script-requests.log');
    this.analyticsFile = path.join(this.logDir, 'dynamic-script-analytics.json');

    // Ensure logs directory exists
    if (!fs.existsSync(this.logDir)) {
      fs.mkdirSync(this.logDir, { recursive: true });
    }
  }

  /**
   * Log a dynamic script generation request
   */
  logRequest(entry: RequestLogEntry): void {
    try {
      const logLine = this.formatLogEntry(entry);
      
      // Append to requests log file
      fs.appendFileSync(this.requestsLogFile, logLine + '\n');
      
      // Update analytics
      this.updateAnalytics(entry);
      
      console.log(`[DYNAMIC_SCRIPT_LOG] ${entry.image_id} - ${entry.success ? 'SUCCESS' : 'FAILED'} - ${entry.response_time}ms`);
      
    } catch (error) {
      console.error('Error logging request:', error);
    }
  }

  /**
   * Format log entry for file storage
   */
  private formatLogEntry(entry: RequestLogEntry): string {
    const fields = [
      entry.timestamp,
      entry.image_id,
      entry.user_id || 'anonymous',
      `"${entry.script.replace(/"/g, '""')}"`, // Escape quotes in script
      entry.language,
      entry.tone,
      entry.response_time.toString(),
      entry.success ? 'SUCCESS' : 'FAILED',
      entry.error ? `"${entry.error.replace(/"/g, '""')}"` : '',
      entry.metadata ? JSON.stringify(entry.metadata) : ''
    ];
    
    return fields.join(' | ');
  }

  /**
   * Update analytics data
   */
  private updateAnalytics(entry: RequestLogEntry): void {
    try {
      let analytics: AnalyticsData;
      
      // Load existing analytics or create new
      if (fs.existsSync(this.analyticsFile)) {
        const data = fs.readFileSync(this.analyticsFile, 'utf8');
        analytics = JSON.parse(data);
      } else {
        analytics = {
          total_requests: 0,
          successful_requests: 0,
          failed_requests: 0,
          average_response_time: 0,
          requests_by_poster: {},
          requests_by_language: {},
          requests_by_tone: {},
          requests_by_hour: {},
          last_updated: new Date().toISOString()
        };
      }

      // Update counters
      analytics.total_requests++;
      if (entry.success) {
        analytics.successful_requests++;
      } else {
        analytics.failed_requests++;
      }

      // Update response time average
      const totalTime = analytics.average_response_time * (analytics.total_requests - 1) + entry.response_time;
      analytics.average_response_time = totalTime / analytics.total_requests;

      // Update poster requests
      analytics.requests_by_poster[entry.image_id] = (analytics.requests_by_poster[entry.image_id] || 0) + 1;

      // Update language requests
      analytics.requests_by_language[entry.language] = (analytics.requests_by_language[entry.language] || 0) + 1;

      // Update tone requests
      analytics.requests_by_tone[entry.tone] = (analytics.requests_by_tone[entry.tone] || 0) + 1;

      // Update hourly requests
      const hour = new Date(entry.timestamp).getHours().toString();
      analytics.requests_by_hour[hour] = (analytics.requests_by_hour[hour] || 0) + 1;

      analytics.last_updated = new Date().toISOString();

      // Save updated analytics
      fs.writeFileSync(this.analyticsFile, JSON.stringify(analytics, null, 2));

    } catch (error) {
      console.error('Error updating analytics:', error);
    }
  }

  /**
   * Get analytics data
   */
  getAnalytics(): AnalyticsData | null {
    try {
      if (fs.existsSync(this.analyticsFile)) {
        const data = fs.readFileSync(this.analyticsFile, 'utf8');
        return JSON.parse(data);
      }
      return null;
    } catch (error) {
      console.error('Error reading analytics:', error);
      return null;
    }
  }

  /**
   * Get recent requests from log file
   */
  getRecentRequests(limit: number = 100): RequestLogEntry[] {
    try {
      if (!fs.existsSync(this.requestsLogFile)) {
        return [];
      }

      const logContent = fs.readFileSync(this.requestsLogFile, 'utf8');
      const lines = logContent.trim().split('\n').filter(line => line.trim());
      
      // Get last N lines
      const recentLines = lines.slice(-limit);
      
      return recentLines.map(line => this.parseLogLine(line)).filter(entry => entry !== null);
      
    } catch (error) {
      console.error('Error reading recent requests:', error);
      return [];
    }
  }

  /**
   * Parse a log line back to RequestLogEntry
   */
  private parseLogLine(line: string): RequestLogEntry | null {
    try {
      const parts = line.split(' | ');
      
      if (parts.length < 8) {
        return null;
      }

      return {
        timestamp: parts[0],
        image_id: parts[1],
        user_id: parts[2] === 'anonymous' ? undefined : parts[2],
        script: parts[3].replace(/^"|"$/g, '').replace(/""/g, '"'), // Unescape quotes
        language: parts[4],
        tone: parts[5],
        response_time: parseInt(parts[6]),
        success: parts[7] === 'SUCCESS',
        error: parts[8] ? parts[8].replace(/^"|"$/g, '').replace(/""/g, '"') : undefined,
        metadata: parts[9] ? JSON.parse(parts[9]) : undefined
      };
    } catch (error) {
      console.error('Error parsing log line:', error);
      return null;
    }
  }

  /**
   * Generate analytics report
   */
  generateReport(): string {
    const analytics = this.getAnalytics();
    
    if (!analytics) {
      return 'No analytics data available.';
    }

    const successRate = ((analytics.successful_requests / analytics.total_requests) * 100).toFixed(1);
    
    let report = `# Dynamic Script Generation Analytics Report\n\n`;
    report += `**Generated:** ${new Date().toISOString()}\n\n`;
    
    report += `## Overview\n\n`;
    report += `- **Total Requests:** ${analytics.total_requests}\n`;
    report += `- **Successful Requests:** ${analytics.successful_requests}\n`;
    report += `- **Failed Requests:** ${analytics.failed_requests}\n`;
    report += `- **Success Rate:** ${successRate}%\n`;
    report += `- **Average Response Time:** ${analytics.average_response_time.toFixed(0)}ms\n\n`;
    
    report += `## Requests by Poster\n\n`;
    const sortedPosters = Object.entries(analytics.requests_by_poster)
      .sort(([,a], [,b]) => b - a);
    
    sortedPosters.forEach(([poster, count]) => {
      report += `- **${poster}:** ${count} requests\n`;
    });
    
    report += `\n## Requests by Language\n\n`;
    Object.entries(analytics.requests_by_language).forEach(([language, count]) => {
      report += `- **${language}:** ${count} requests\n`;
    });
    
    report += `\n## Requests by Tone\n\n`;
    Object.entries(analytics.requests_by_tone).forEach(([tone, count]) => {
      report += `- **${tone}:** ${count} requests\n`;
    });
    
    report += `\n## Hourly Distribution\n\n`;
    Object.entries(analytics.requests_by_hour)
      .sort(([a], [b]) => parseInt(a) - parseInt(b))
      .forEach(([hour, count]) => {
        report += `- **${hour}:00:** ${count} requests\n`;
      });
    
    return report;
  }

  /**
   * Clear old logs (keep last N entries)
   */
  clearOldLogs(keepEntries: number = 1000): void {
    try {
      const recentRequests = this.getRecentRequests(keepEntries);
      
      // Clear the log file
      fs.writeFileSync(this.requestsLogFile, '');
      
      // Rewrite with recent entries
      recentRequests.forEach(entry => {
        const logLine = this.formatLogEntry(entry);
        fs.appendFileSync(this.requestsLogFile, logLine + '\n');
      });
      
      console.log(`Cleared old logs, kept ${recentRequests.length} recent entries`);
      
    } catch (error) {
      console.error('Error clearing old logs:', error);
    }
  }
}

// Export singleton instance
export const dynamicScriptLogger = new DynamicScriptLogger();

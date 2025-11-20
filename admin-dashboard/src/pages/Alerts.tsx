import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Paper,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Chip,
  LinearProgress,
  Alert as MuiAlert,
} from "@mui/material";
import {
  CheckCircle,
  Warning,
  Error as ErrorIcon,
  Info,
} from "@mui/icons-material";
import { AnalyticsService, AggregatedMetric } from "../services/analyticsService";

interface AlertItem {
  id: string;
  severity: "success" | "warning" | "error" | "info";
  title: string;
  message: string;
  timestamp: Date;
}

const Alerts: React.FC = () => {
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    checkSystemStatus();
  }, []);

  const checkSystemStatus = async () => {
    setLoading(true);
    try {
      const response = await AnalyticsService.getAggregatedMetrics();
      const metrics = response.data;
      
      const newAlerts: AlertItem[] = [];
      const today = new Date();

      // Check if we have data for today
      const todayStr = today.toISOString().split('T')[0];
      const todayMetric = metrics.find(m => m.date === todayStr);

      if (todayMetric) {
        // Check latency
        if (todayMetric.avg_latency_ms > 5000) {
          newAlerts.push({
            id: "latency-high",
            severity: "error",
            title: "High Latency Detected",
            message: `Average latency is ${todayMetric.avg_latency_ms}ms, exceeding the 5000ms threshold.`,
            timestamp: today,
          });
        } else if (todayMetric.avg_latency_ms > 2000) {
          newAlerts.push({
            id: "latency-warn",
            severity: "warning",
            title: "Elevated Latency",
            message: `Average latency is ${todayMetric.avg_latency_ms}ms.`,
            timestamp: today,
          });
        } else {
           newAlerts.push({
            id: "latency-ok",
            severity: "success",
            title: "Latency Normal",
            message: `Average latency is ${todayMetric.avg_latency_ms}ms.`,
            timestamp: today,
          });
        }

        // Check feedback
        const totalFeedback = todayMetric.likes + todayMetric.dislikes;
        if (totalFeedback > 0) {
          const dislikeRate = todayMetric.dislikes / totalFeedback;
          if (dislikeRate > 0.5) {
             newAlerts.push({
              id: "feedback-neg",
              severity: "warning",
              title: "Negative Feedback Trend",
              message: `Dislike rate is ${(dislikeRate * 100).toFixed(1)}%.`,
              timestamp: today,
            });
          }
        }
      } else {
        newAlerts.push({
          id: "no-data",
          severity: "info",
          title: "No Data for Today",
          message: "Waiting for interactions to generate metrics.",
          timestamp: today,
        });
      }

      setAlerts(newAlerts);
    } catch (err) {
      console.error("Failed to check system status:", err);
    } finally {
      setLoading(false);
    }
  };

  const getIcon = (severity: string) => {
    switch (severity) {
      case "success": return <CheckCircle color="success" />;
      case "warning": return <Warning color="warning" />;
      case "error": return <ErrorIcon color="error" />;
      default: return <Info color="info" />;
    }
  };

  if (loading) return <LinearProgress />;

  return (
    <Box>
      <Typography variant="h4" gutterBottom>System Alerts</Typography>
      
      <Paper sx={{ p: 2 }}>
        {alerts.length === 0 ? (
          <MuiAlert severity="success">All systems operational. No active alerts.</MuiAlert>
        ) : (
          <List>
            {alerts.map((alert) => (
              <ListItem key={alert.id} divider>
                <ListItemIcon>
                  {getIcon(alert.severity)}
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Box display="flex" alignItems="center" gap={1}>
                      <Typography variant="subtitle1">{alert.title}</Typography>
                      <Chip 
                        label={alert.severity.toUpperCase()} 
                        color={alert.severity as any} 
                        size="small" 
                        variant="outlined"
                      />
                    </Box>
                  }
                  secondary={
                    <>
                      <Typography component="span" variant="body2" color="text.primary">
                        {alert.message}
                      </Typography>
                      <br />
                      <Typography component="span" variant="caption" color="text.secondary">
                        {alert.timestamp.toLocaleString()}
                      </Typography>
                    </>
                  }
                />
              </ListItem>
            ))}
          </List>
        )}
      </Paper>
    </Box>
  );
};

export default Alerts;

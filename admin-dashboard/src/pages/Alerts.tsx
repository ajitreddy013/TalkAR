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
  FormControlLabel,
  Switch,
  Button,
  Stack,
} from "@mui/material";
import {
  CheckCircle,
  Warning,
  Error as ErrorIcon,
  Info,
} from "@mui/icons-material";
import { ImageService } from "../services/imageService";

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
  const [runtimeMode, setRuntimeMode] = useState<string>("normal");
  const [toggles, setToggles] = useState<{
    disableEnqueue: boolean;
    forceReadyOnly: boolean;
    enableFallback: boolean;
  }>({
    disableEnqueue: false,
    forceReadyOnly: false,
    enableFallback: false,
  });

  const updateToggle = async (key: "disableEnqueue" | "forceReadyOnly" | "enableFallback", value: boolean) => {
    setLoading(true);
    try {
      await ImageService.setPosterOpsToggles({
        [key]: value,
        actor: "admin_dashboard",
      });
      await checkSystemStatus();
    } catch (err) {
      console.error("Failed updating runtime toggle", err);
      setLoading(false);
    }
  };

  useEffect(() => {
    checkSystemStatus();
  }, []);

  const checkSystemStatus = async () => {
    setLoading(true);
    try {
      const [alertsResponse, togglesResponse] = await Promise.all([
        ImageService.getPosterOpsAlerts(),
        ImageService.getPosterOpsToggles(),
      ]);

      const payload = alertsResponse.data;
      const togglesPayload = togglesResponse.data;
      setRuntimeMode(payload.runtimeMode);
      setToggles({
        disableEnqueue: !!togglesPayload?.effective?.disableEnqueue,
        forceReadyOnly: !!togglesPayload?.effective?.forceReadyOnly,
        enableFallback: !!togglesPayload?.effective?.enableFallback,
      });

      const newAlerts: AlertItem[] = [];
      const today = new Date();
      payload.alerts.forEach((a) => {
        const severity: AlertItem["severity"] = a.active ? a.severity : "success";
        newAlerts.push({
          id: a.key,
          severity,
          title: a.key.replace(/_/g, " ").toUpperCase(),
          message: a.message,
          timestamp: today,
        });
      });
      if (newAlerts.length === 0) {
        newAlerts.push({
          id: "no-alerts",
          severity: "success",
          title: "No Active Alerts",
          message: "All runtime alert checks are healthy.",
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
      <Box display="flex" gap={1} mb={2}>
        <Chip label={`Runtime: ${runtimeMode}`} color="primary" variant="outlined" />
        <Chip label={`Enqueue: ${toggles.disableEnqueue ? "OFF" : "ON"}`} />
        <Chip label={`Ready-Only: ${toggles.forceReadyOnly ? "ON" : "OFF"}`} />
        <Chip label={`Fallback: ${toggles.enableFallback ? "ON" : "OFF"}`} />
      </Box>
      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="h6" gutterBottom>Emergency Runtime Controls</Typography>
        <Stack direction="row" gap={2} flexWrap="wrap">
          <FormControlLabel
            control={
              <Switch
                checked={toggles.disableEnqueue}
                onChange={(_, checked) => updateToggle("disableEnqueue", checked)}
              />
            }
            label="Disable Enqueue"
          />
          <FormControlLabel
            control={
              <Switch
                checked={toggles.forceReadyOnly}
                onChange={(_, checked) => updateToggle("forceReadyOnly", checked)}
              />
            }
            label="Ready-Only Mode"
          />
          <FormControlLabel
            control={
              <Switch
                checked={toggles.enableFallback}
                onChange={(_, checked) => updateToggle("enableFallback", checked)}
              />
            }
            label="Enable Fallback"
          />
          <Button variant="outlined" onClick={checkSystemStatus}>Refresh</Button>
        </Stack>
      </Paper>
      
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
                        color={alert.severity}
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

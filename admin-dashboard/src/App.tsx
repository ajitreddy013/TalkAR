import React, { useState } from "react";
import {
  AppBar,
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from "@mui/material";
import {
  Dashboard as DashboardIcon,
  Image as ImageIcon,
  Settings as SettingsIcon,
  Analytics as AnalyticsIcon,
  Tune as TuneIcon,
  TableChart,
  MonitorHeart,
  Notifications,
} from "@mui/icons-material";
import ImagesPage from "./pages/Images";
import AIConfigPage from "./pages/AIConfig";
import AnalyticsPage from "./pages/Analytics";
import SettingsPage from "./pages/Settings";
import DashboardPage from "./pages/Dashboard";
import InteractionsPage from "./pages/Interactions";
import LiveMonitorPage from "./pages/LiveMonitor";
import AlertsPage from "./pages/Alerts";

function App() {
  const [currentView, setCurrentView] = useState("dashboard");

  const menuItems = [
    { id: "dashboard", label: "Dashboard", icon: <DashboardIcon /> },
    { id: "interactions", label: "Interactions", icon: <TableChart /> },
    { id: "live-monitor", label: "Live Monitor", icon: <MonitorHeart /> },
    { id: "alerts", label: "Alerts", icon: <Notifications /> },
    { id: "analytics", label: "Analytics (Legacy)", icon: <AnalyticsIcon /> },
    { id: "images", label: "Images", icon: <ImageIcon /> },
    { id: "ai-config", label: "AI Config", icon: <SettingsIcon /> },
    { id: "settings", label: "Settings", icon: <TuneIcon /> },
  ];

  return (
    <Box sx={{ display: "flex", flexDirection: "column", minHeight: "100vh" }}>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            TalkAR Admin Dashboard
          </Typography>
        </Toolbar>
      </AppBar>

      <Box sx={{ display: "flex", flex: 1 }}>
        <Box sx={{ width: 250, bgcolor: "grey.100", p: 2 }}>
          <List>
            {menuItems.map((item) => (
              <ListItem key={item.id} disablePadding>
                <ListItemButton
                  selected={currentView === item.id}
                  onClick={() => setCurrentView(item.id)}
                >
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.label} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Box>

        <Box sx={{ flex: 1, p: 3 }}>
          {currentView === "dashboard" && <DashboardPage />}
          {currentView === "interactions" && <InteractionsPage />}
          {currentView === "live-monitor" && <LiveMonitorPage />}
          {currentView === "alerts" && <AlertsPage />}
          {currentView === "analytics" && <AnalyticsPage />}
          {currentView === "images" && <ImagesPage />}
          {currentView === "ai-config" && <AIConfigPage />}
          {currentView === "settings" && <SettingsPage />}
        </Box>
      </Box>
    </Box>
  );
}

export default App;

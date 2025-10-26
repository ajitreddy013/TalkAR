import React, { useState } from "react";
import {
  AppBar,
  Box,
  Button,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Paper,
  Toolbar,
  Typography,
} from "@mui/material";
import {
  Dashboard as DashboardIcon,
  Image as ImageIcon,
  Settings as SettingsIcon,
  Analytics as AnalyticsIcon,
  Tune as TuneIcon,
} from "@mui/icons-material";
import ImagesPage from "./pages/Images";
import AIConfigPage from "./pages/AIConfig";
import AnalyticsPage from "./pages/Analytics";
import SettingsPage from "./pages/Settings";
import DashboardPage from "./pages/Dashboard";

function App() {
  const [currentView, setCurrentView] = useState("dashboard");

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
            <ListItem disablePadding>
              <ListItemButton
                selected={currentView === "dashboard"}
                onClick={() => setCurrentView("dashboard")}
              >
                <ListItemIcon>
                  <DashboardIcon />
                </ListItemIcon>
                <ListItemText primary="Dashboard" />
              </ListItemButton>
            </ListItem>
            <ListItem disablePadding>
              <ListItemButton
                selected={currentView === "analytics"}
                onClick={() => setCurrentView("analytics")}
              >
                <ListItemIcon>
                  <AnalyticsIcon />
                </ListItemIcon>
                <ListItemText primary="Analytics" />
              </ListItemButton>
            </ListItem>
            <ListItem disablePadding>
              <ListItemButton
                selected={currentView === "images"}
                onClick={() => setCurrentView("images")}
              >
                <ListItemIcon>
                  <ImageIcon />
                </ListItemIcon>
                <ListItemText primary="Images" />
              </ListItemButton>
            </ListItem>
            <ListItem disablePadding>
              <ListItemButton
                selected={currentView === "ai-config"}
                onClick={() => setCurrentView("ai-config")}
              >
                <ListItemIcon>
                  <SettingsIcon />
                </ListItemIcon>
                <ListItemText primary="AI Config" />
              </ListItemButton>
            </ListItem>
            <ListItem disablePadding>
              <ListItemButton
                selected={currentView === "settings"}
                onClick={() => setCurrentView("settings")}
              >
                <ListItemIcon>
                  <TuneIcon />
                </ListItemIcon>
                <ListItemText primary="Settings" />
              </ListItemButton>
            </ListItem>
          </List>
        </Box>

        <Box sx={{ flex: 1, p: 3 }}>
          {currentView === "dashboard" && <DashboardPage />}
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

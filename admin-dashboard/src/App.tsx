import React, { useState, useEffect } from "react";
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
} from "@mui/icons-material";
import ImagesPage from "./pages/Images";
import AIConfigPage from "./pages/AIConfig";

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
          </List>
        </Box>

        <Box sx={{ flex: 1, p: 3 }}>
          {currentView === "dashboard" && (
            <Paper sx={{ p: 3 }}>
              <Typography variant="h4" gutterBottom>
                TalkAR Admin Dashboard
              </Typography>
              <Typography variant="body1" paragraph>
                Welcome to the TalkAR Admin Dashboard. Here you can manage
                images and scripts for your AR application.
              </Typography>
              <Button
                variant="contained"
                onClick={() => setCurrentView("images")}
                sx={{ mr: 2 }}
              >
                Manage Images
              </Button>
              <Button
                variant="outlined"
                onClick={() => setCurrentView("ai-config")}
                sx={{ mr: 2 }}
              >
                AI Configuration
              </Button>
            </Paper>
          )}

          {currentView === "images" && <ImagesPage />}
          {currentView === "ai-config" && <AIConfigPage />}
        </Box>
      </Box>
    </Box>
  );
}

export default App;

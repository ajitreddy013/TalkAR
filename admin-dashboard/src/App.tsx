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
} from "@mui/icons-material";
import ImagesPage from "./pages/Images";

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
            </Paper>
          )}

          {currentView === "images" && <ImagesPage />}
        </Box>
      </Box>
    </Box>
  );
}

export default App;

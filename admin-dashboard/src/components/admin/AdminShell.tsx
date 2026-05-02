import React from "react";
import {
  AppBar,
  Box,
  CssBaseline,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from "@mui/material";
import {
  Menu as MenuIcon,
  Dashboard,
  Image,
  TableChart,
  MonitorHeart,
  Notifications,
  Analytics,
  Settings,
  Tune,
} from "@mui/icons-material";
import { NavLink, Outlet } from "react-router-dom";

const drawerWidth = 260;

const menu = [
  { to: "/dashboard", label: "Dashboard", icon: <Dashboard fontSize="small" /> },
  { to: "/images", label: "Images", icon: <Image fontSize="small" /> },
  { to: "/interactions", label: "Interactions", icon: <TableChart fontSize="small" /> },
  { to: "/live-monitor", label: "Live Monitor", icon: <MonitorHeart fontSize="small" /> },
  { to: "/alerts", label: "Alerts", icon: <Notifications fontSize="small" /> },
  { to: "/analytics", label: "Analytics", icon: <Analytics fontSize="small" /> },
  { to: "/ai-config", label: "AI Config", icon: <Settings fontSize="small" /> },
  { to: "/settings", label: "Settings", icon: <Tune fontSize="small" /> },
];

export default function AdminShell() {
  const [mobileOpen, setMobileOpen] = React.useState(false);

  const drawerContent = (
    <Box sx={{ p: 1.5 }}>
      <Typography variant="h6" sx={{ p: 1.5 }}>
        TalkAR Admin
      </Typography>
      <List>
        {menu.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            style={{ textDecoration: "none", color: "inherit" }}
            onClick={() => setMobileOpen(false)}
          >
            {({ isActive }) => (
              <ListItemButton
                selected={isActive}
                sx={{ borderRadius: 2, mb: 0.5 }}
              >
                <ListItemIcon sx={{ minWidth: 36 }}>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            )}
          </NavLink>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: "flex", minHeight: "100vh", bgcolor: "background.default" }}>
      <CssBaseline />
      <AppBar
        color="inherit"
        elevation={0}
        sx={{
          borderBottom: "1px solid",
          borderColor: "divider",
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
        }}
      >
        <Toolbar>
          <IconButton edge="start" onClick={() => setMobileOpen(true)} sx={{ mr: 1, display: { md: "none" } }}>
            <MenuIcon />
          </IconButton>
          <Typography variant="h6">Operations Console</Typography>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{ display: { xs: "block", md: "none" }, "& .MuiDrawer-paper": { width: drawerWidth } }}
        >
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          open
          sx={{ display: { xs: "none", md: "block" }, "& .MuiDrawer-paper": { width: drawerWidth, borderRight: "1px solid #e5e7eb" } }}
        >
          {drawerContent}
        </Drawer>
      </Box>

      <Box component="main" sx={{ flexGrow: 1, p: 3, width: { md: `calc(100% - ${drawerWidth}px)` } }}>
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}

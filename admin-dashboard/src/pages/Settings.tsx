import React from "react";
import { Typography, Paper } from "@mui/material";

export default function Settings() {
  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>
      <Typography variant="body1">
        Configure your TalkAR admin settings. This feature will be available
        soon.
      </Typography>
    </Paper>
  );
}

import React from "react";
import { Typography, Paper } from "@mui/material";

export default function Analytics() {
  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Analytics
      </Typography>
      <Typography variant="body1">
        View analytics and usage statistics for your AR content. This feature
        will be available soon.
      </Typography>
    </Paper>
  );
}

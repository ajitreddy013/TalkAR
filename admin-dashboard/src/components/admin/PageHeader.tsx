import { Box, Typography } from "@mui/material";
import React from "react";

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
}

export default function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  return (
    <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 2, mb: 3 }}>
      <Box>
        <Typography variant="h4" gutterBottom>
          {title}
        </Typography>
        {subtitle ? <Typography color="text.secondary">{subtitle}</Typography> : null}
      </Box>
      {actions}
    </Box>
  );
}

import { Card, CardContent, Typography, Box } from "@mui/material";
import React from "react";

export default function StatCard({
  title,
  value,
  icon,
}: {
  title: string;
  value: string | number;
  icon?: React.ReactNode;
}) {
  return (
    <Card>
      <CardContent>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Box>
            <Typography color="text.secondary" variant="body2">
              {title}
            </Typography>
            <Typography variant="h5" sx={{ mt: 1 }}>
              {value}
            </Typography>
          </Box>
          {icon}
        </Box>
      </CardContent>
    </Card>
  );
}

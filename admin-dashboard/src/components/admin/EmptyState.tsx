import { Box, Button, Typography } from "@mui/material";

export default function EmptyState({
  title,
  description,
  actionLabel,
  onAction,
}: {
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <Box sx={{ py: 8, textAlign: "center" }}>
      <Typography variant="h6" gutterBottom>
        {title}
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 2 }}>
        {description}
      </Typography>
      {actionLabel && onAction ? <Button variant="contained" onClick={onAction}>{actionLabel}</Button> : null}
    </Box>
  );
}

import { Alert, Button, Stack } from "@mui/material";

export default function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  return (
    <Stack spacing={2}>
      <Alert severity="error">{message}</Alert>
      {onRetry ? <Button variant="outlined" onClick={onRetry}>Retry</Button> : null}
    </Stack>
  );
}

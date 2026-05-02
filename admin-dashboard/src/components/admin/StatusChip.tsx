import { Chip } from "@mui/material";

export type ArtifactStatus = "queued" | "processing" | "ready" | "failed";

export default function StatusChip({ status }: { status: ArtifactStatus }) {
  if (status === "ready") return <Chip size="small" color="success" label="Ready" />;
  if (status === "failed") return <Chip size="small" color="error" label="Failed" />;
  if (status === "processing") return <Chip size="small" color="warning" label="Processing" />;
  return <Chip size="small" color="default" label="Queued" />;
}

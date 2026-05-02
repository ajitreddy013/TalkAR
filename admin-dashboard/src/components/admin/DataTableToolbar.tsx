import { Box, Button, Stack, TextField } from "@mui/material";
import React from "react";

interface Props {
  search: string;
  onSearchChange: (value: string) => void;
  left?: React.ReactNode;
  right?: React.ReactNode;
  onAdd?: () => void;
  addLabel?: string;
}

export default function DataTableToolbar({
  search,
  onSearchChange,
  left,
  right,
  onAdd,
  addLabel = "Add",
}: Props) {
  return (
    <Stack direction={{ xs: "column", md: "row" }} spacing={2} sx={{ mb: 2 }} alignItems={{ md: "center" }}>
      <TextField
        size="small"
        label="Search"
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        sx={{ minWidth: 260 }}
      />
      {left}
      <Box sx={{ flex: 1 }} />
      {right}
      {onAdd ? (
        <Button variant="contained" onClick={onAdd}>
          {addLabel}
        </Button>
      ) : null}
    </Stack>
  );
}

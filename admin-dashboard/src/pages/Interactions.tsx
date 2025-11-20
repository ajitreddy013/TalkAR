import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Paper,
  Button,
  LinearProgress,
  Alert,
} from "@mui/material";
import { DataGrid, GridColDef, GridToolbar } from "@mui/x-data-grid";
import { Download as DownloadIcon } from "@mui/icons-material";
import { AnalyticsService, Interaction } from "../services/analyticsService";

const Interactions: React.FC = () => {
  const [interactions, setInteractions] = useState<Interaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [rowCount, setRowCount] = useState(0);

  useEffect(() => {
    fetchInteractions(page, pageSize);
  }, [page, pageSize]);

  const fetchInteractions = async (p: number, s: number) => {
    setLoading(true);
    try {
      // API uses 1-based page index
      const response = await AnalyticsService.getInteractions(p + 1, s);
      setInteractions(response.data.interactions);
      setRowCount(response.data.total);
    } catch (err) {
      console.error("Failed to fetch interactions:", err);
      setError("Failed to load interactions");
    } finally {
      setLoading(false);
    }
  };

  const handleExport = () => {
    AnalyticsService.exportInteractions();
  };

  const columns: GridColDef[] = [
    { field: "id", headerName: "ID", width: 90 },
    { field: "poster_id", headerName: "Poster ID", width: 150 },
    { field: "status", headerName: "Status", width: 120 },
    { 
      field: "latency_ms", 
      headerName: "Latency (ms)", 
      width: 130,
      type: 'number',
    },
    { field: "feedback", headerName: "Feedback", width: 120 },
    { 
      field: "created_at", 
      headerName: "Timestamp", 
      width: 200,
      valueFormatter: (params: any) => new Date(params.value as string).toLocaleString(),
    },
    {
      field: "script",
      headerName: "Script",
      width: 300,
    },
  ];

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Interactions</Typography>
        <Button
          variant="contained"
          startIcon={<DownloadIcon />}
          onClick={handleExport}
        >
          Export CSV
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Paper sx={{ height: 600, width: "100%" }}>
        <DataGrid
          rows={interactions}
          columns={columns}
          paginationMode="server"
          rowCount={rowCount}
          loading={loading}
          pageSizeOptions={[10, 25, 50]}
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(model) => {
            setPage(model.page);
            setPageSize(model.pageSize);
          }}
          disableRowSelectionOnClick
          slots={{ toolbar: GridToolbar }}
        />
      </Paper>
    </Box>
  );
};

export default Interactions;

import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Paper,
  Button,
  LinearProgress,
  Alert,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Grid,
} from "@mui/material";
import { DataGrid, GridColDef, GridToolbar } from "@mui/x-data-grid";
import { Download as DownloadIcon, FilterList as FilterIcon } from "@mui/icons-material";
import { AnalyticsService, Interaction } from "../services/analyticsService";

const Interactions: React.FC = () => {
  const [interactions, setInteractions] = useState<Interaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [rowCount, setRowCount] = useState(0);

  // Filter state
  const [statusFilter, setStatusFilter] = useState("all");
  const [posterIdFilter, setPosterIdFilter] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  useEffect(() => {
    fetchInteractions(page, pageSize);
  }, [page, pageSize]);

  const fetchInteractions = async (p: number, s: number) => {
    setLoading(true);
    try {
      // API uses 1-based page index
      const filters = {
        status: statusFilter !== "all" ? statusFilter : undefined,
        poster_id: posterIdFilter || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      };
      
      const response = await AnalyticsService.getInteractions(p + 1, s, filters);
      setInteractions(response.data.interactions);
      setRowCount(response.data.total);
    } catch (err) {
      console.error("Failed to fetch interactions:", err);
      setError("Failed to load interactions");
    } finally {
      setLoading(false);
    }
  };

  const handleApplyFilters = () => {
    setPage(0); // Reset to first page when filtering
    fetchInteractions(0, pageSize);
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

      {/* Filter Bar */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6} md={2}>
            <FormControl fullWidth size="small">
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="all">All</MenuItem>
                <MenuItem value="completed">Completed</MenuItem>
                <MenuItem value="failed">Failed</MenuItem>
                <MenuItem value="started">Started</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <TextField
              fullWidth
              size="small"
              label="Poster ID"
              value={posterIdFilter}
              onChange={(e) => setPosterIdFilter(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField
              fullWidth
              size="small"
              label="Start Date"
              type="date"
              InputLabelProps={{ shrink: true }}
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField
              fullWidth
              size="small"
              label="End Date"
              type="date"
              InputLabelProps={{ shrink: true }}
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} md={3}>
            <Button
              variant="contained"
              startIcon={<FilterIcon />}
              onClick={handleApplyFilters}
              fullWidth
            >
              Apply Filters
            </Button>
          </Grid>
        </Grid>
      </Paper>

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

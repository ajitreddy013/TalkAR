import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  Chip,
  LinearProgress,
  MenuItem,
  Paper,
  Select,
  Stack,
  Step,
  StepLabel,
  Stepper,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import {
  Delete,
  Edit,
  Refresh,
  Visibility,
  Upload,
} from "@mui/icons-material";
import {
  DataGrid,
  GridColDef,
  GridPaginationModel,
  GridRowSelectionModel,
  GridSortModel,
} from "@mui/x-data-grid";
import PageHeader from "../components/admin/PageHeader";
import DataTableToolbar from "../components/admin/DataTableToolbar";
import EmptyState from "../components/admin/EmptyState";
import ErrorState from "../components/admin/ErrorState";
import ConfirmDialog from "../components/admin/ConfirmDialog";
import StatusChip from "../components/admin/StatusChip";
import {
  ArtifactStatus,
  Image,
  ImageListQuery,
  ImageService,
} from "../services/imageService";

const API_URL = process.env.REACT_APP_API_URL || "http://localhost:4000";
const uploadSteps = ["Image", "Metadata", "Script", "Review"];

function getImageUrl(path: string) {
  if (!path) return "";
  return path.startsWith("/") ? `${API_URL}${path}` : path;
}

export default function ImagesPage() {
  const [images, setImages] = useState<Image[]>([]);
  const [totalRows, setTotalRows] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"all" | ArtifactStatus>("all");
  const [confidenceMin, setConfidenceMin] = useState<string>("");
  const [confidenceMax, setConfidenceMax] = useState<string>("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [sortModel, setSortModel] = useState<GridSortModel>([
    { field: "updatedAt", sort: "desc" },
  ]);
  const [rowSelectionModel, setRowSelectionModel] = useState<GridRowSelectionModel>([]);

  const [drawerImage, setDrawerImage] = useState<Image | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Image | null>(null);
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false);
  const [bulkRetryOpen, setBulkRetryOpen] = useState(false);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [uploadStep, setUploadStep] = useState(0);
  const [isEditMode, setIsEditMode] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [uploadState, setUploadState] = useState<"idle" | "uploading" | "generating" | "ready" | "failed">("idle");

  const [uploadName, setUploadName] = useState("");
  const [uploadDescription, setUploadDescription] = useState("");
  const [uploadScript, setUploadScript] = useState("");
  const [selectedMainFile, setSelectedMainFile] = useState<File | null>(null);
  const [selectedThumbFile, setSelectedThumbFile] = useState<File | null>(null);

  const fetchImages = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const query: ImageListQuery = {
        search: search.trim() || undefined,
        status: statusFilter === "all" ? undefined : statusFilter,
        page: page + 1,
        pageSize,
        confidenceMin: confidenceMin === "" ? undefined : Number(confidenceMin),
        confidenceMax: confidenceMax === "" ? undefined : Number(confidenceMax),
      };

      const sort = sortModel[0];
      if (sort?.sort) {
        const sortByMap: Record<string, ImageListQuery["sortBy"]> = {
          name: "name",
          updatedAt: "updatedAt",
          confidence: "confidence",
          artifactStatus: "artifactStatus",
        };
        query.sortBy = sortByMap[sort.field] || "updatedAt";
        query.sortDir = sort.sort;
      }

      const response = await ImageService.listImages(query);
      const payload = response.data as unknown;
      if (Array.isArray(payload)) {
        setImages(payload as Image[]);
        setTotalRows((payload as Image[]).length);
      } else if (
        payload &&
        typeof payload === "object" &&
        "items" in payload &&
        Array.isArray((payload as { items: unknown }).items)
      ) {
        const typed = payload as { items: Image[]; total?: number };
        setImages(typed.items);
        setTotalRows(typed.total ?? typed.items.length);
      } else if (
        payload &&
        typeof payload === "object" &&
        "data" in payload &&
        Array.isArray((payload as { data: unknown }).data)
      ) {
        const typed = payload as { data: Image[]; total?: number };
        setImages(typed.data);
        setTotalRows(typed.total ?? typed.data.length);
      } else {
        setImages([]);
        setTotalRows(0);
      }
    } catch (e) {
      console.error(e);
      setError("Failed to load images.");
    } finally {
      setLoading(false);
    }
  }, [search, statusFilter, confidenceMin, confidenceMax, page, pageSize, sortModel]);

  useEffect(() => {
    fetchImages();
  }, [fetchImages]);

  const filteredImages = useMemo(() => {
    if (search || statusFilter !== "all" || confidenceMin !== "" || confidenceMax !== "") {
      return images;
    }
    const minConfidence = confidenceMin === "" ? null : Number(confidenceMin);
    const maxConfidence = confidenceMax === "" ? null : Number(confidenceMax);
    return images.filter((img) => {
      const status = img.talkingPhotoArtifact?.status || "queued";
      const confidence = img.talkingPhotoArtifact?.confidence ?? null;
      const matchesSearch =
        img.name.toLowerCase().includes(search.toLowerCase()) ||
        (img.description || "").toLowerCase().includes(search.toLowerCase());
      const matchesStatus = statusFilter === "all" ? true : status === statusFilter;
      const matchesMinConfidence = minConfidence == null ? true : (confidence ?? 0) >= minConfidence;
      const matchesMaxConfidence = maxConfidence == null ? true : (confidence ?? 0) <= maxConfidence;
      const matchesConfidence = matchesMinConfidence && matchesMaxConfidence;
      return matchesSearch && matchesStatus && matchesConfidence;
    });
  }, [images, search, statusFilter, confidenceMin, confidenceMax]);

  const openCreateDialog = () => {
    setDialogOpen(true);
    setUploadStep(0);
    setIsEditMode(false);
    setEditId(null);
    setUploadState("idle");
    setUploadName("");
    setUploadDescription("");
    setUploadScript("");
    setSelectedMainFile(null);
    setSelectedThumbFile(null);
  };

  const openEditDialog = (img: Image) => {
    setDialogOpen(true);
    setUploadStep(0);
    setIsEditMode(true);
    setEditId(img.id);
    setUploadState("idle");
    setUploadName(img.name);
    setUploadDescription(img.description || "");
    setUploadScript(img.dialogues?.find((d) => d.isDefault)?.text || "");
    setSelectedMainFile(null);
    setSelectedThumbFile(null);
  };

  const closeDialog = () => {
    setDialogOpen(false);
  };

  const validateStep = () => {
    if (uploadStep === 0 && !isEditMode && !selectedMainFile) return false;
    if (uploadStep === 1 && !uploadName.trim()) return false;
    if (uploadStep === 2 && !uploadScript.trim()) return false;
    return true;
  };

  const handleSubmitUpload = async () => {
    if (!uploadName.trim() || !uploadScript.trim()) return;
    if (!isEditMode && !selectedMainFile) return;

    setUploadState("uploading");
    const formData = new FormData();
    if (selectedMainFile) formData.append("image", selectedMainFile);
    if (selectedThumbFile) formData.append("thumbnail", selectedThumbFile);
    formData.append("name", uploadName);
    formData.append("description", uploadDescription);
    formData.append("script", uploadScript);

    try {
      if (isEditMode && editId) {
        await ImageService.updateImage(editId, formData);
      } else {
        await ImageService.createImage(formData);
      }
      setUploadState("generating");
      await fetchImages();
      setUploadState("ready");
      closeDialog();
    } catch (e) {
      console.error(e);
      setUploadState("failed");
    }
  };

  const handleDelete = async (id: string) => {
    await ImageService.deleteImage(id);
    await fetchImages();
  };

  const handleRetry = async (id: string) => {
    await ImageService.retryTalkingPhoto(id);
    await fetchImages();
  };

  const handleBulkDelete = async () => {
    const ids = rowSelectionModel as string[];
    for (const id of ids) {
      await ImageService.deleteImage(id);
    }
    setRowSelectionModel([]);
    setBulkDeleteOpen(false);
    await fetchImages();
  };

  const handleBulkRetry = async () => {
    const ids = rowSelectionModel as string[];
    await ImageService.bulkRetryTalkingPhoto(ids);
    setBulkRetryOpen(false);
    await fetchImages();
  };

  const handleBulkSetActive = async (isActive: boolean) => {
    const ids = rowSelectionModel as string[];
    await Promise.all(ids.map((id) => ImageService.setActive(id, isActive)));
    await fetchImages();
  };

  const columns: GridColDef[] = [
    {
      field: "name",
      headerName: "Name",
      flex: 1,
      minWidth: 180,
      renderCell: ({ row }) => (
        <Stack spacing={0.5}>
          <Typography variant="body2" fontWeight={600}>{row.name}</Typography>
          <Typography variant="caption" color="text.secondary">{row.description || "No description"}</Typography>
        </Stack>
      ),
    },
    {
      field: "artifactStatus",
      headerName: "Artifact",
      width: 140,
      sortable: false,
      renderCell: ({ row }) => (
        <StatusChip
          status={
            (row.talkingPhotoArtifact?.status || "queued") as
              | "queued"
              | "processing"
              | "ready"
              | "failed"
          }
        />
      ),
    },
    {
      field: "confidence",
      headerName: "Confidence",
      width: 130,
      valueGetter: (_v, row) => row.talkingPhotoArtifact?.confidence ?? null,
      renderCell: ({ value }) => value == null ? <Typography variant="caption">-</Typography> : <Typography variant="body2">{Math.round(Number(value) * 100)}%</Typography>,
    },
    {
      field: "eligible",
      headerName: "Eligible",
      width: 120,
      sortable: false,
      renderCell: ({ row }) => {
        const eligible = !!row.preprocessResult?.eligibleForTalkingPhoto;
        return (
          <Chip
            size="small"
            color={eligible ? "success" : "default"}
            label={eligible ? "Eligible" : "Not Eligible"}
          />
        );
      },
    },
    {
      field: "updatedAt",
      headerName: "Updated",
      width: 180,
      valueFormatter: (value: string) => new Date(value).toLocaleString(),
    },
    {
      field: "actions",
      headerName: "Actions",
      width: 180,
      sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="View">
            <IconButton size="small" onClick={() => setDrawerImage(row)}><Visibility fontSize="small" /></IconButton>
          </Tooltip>
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => openEditDialog(row)}><Edit fontSize="small" /></IconButton>
          </Tooltip>
          <Tooltip title="Retry">
            <IconButton size="small" onClick={() => handleRetry(row.id)} disabled={row.talkingPhotoArtifact?.status !== "failed"}><Refresh fontSize="small" /></IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton size="small" color="error" onClick={() => setDeleteTarget(row)}><Delete fontSize="small" /></IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Image Management"
        subtitle="Manage posters, scripts, and artifact generation lifecycle."
      />

      {loading ? <LinearProgress sx={{ mb: 2 }} /> : null}
      {error ? <ErrorState message={error} onRetry={fetchImages} /> : null}

      <Paper sx={{ p: 2 }}>
        <DataTableToolbar
          search={search}
          onSearchChange={setSearch}
          onAdd={openCreateDialog}
          addLabel="Add Image"
          left={
            <Stack direction="row" spacing={1}>
              <FormControl size="small" sx={{ minWidth: 140 }}>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) =>
                    setStatusFilter(
                      e.target.value as "all" | ArtifactStatus
                    )
                  }
                >
                  <MenuItem value="all">All</MenuItem>
                  <MenuItem value="queued">Queued</MenuItem>
                  <MenuItem value="processing">Processing</MenuItem>
                  <MenuItem value="ready">Ready</MenuItem>
                  <MenuItem value="failed">Failed</MenuItem>
                </Select>
              </FormControl>
              <TextField size="small" label="Min Conf" value={confidenceMin} onChange={(e) => setConfidenceMin(e.target.value)} sx={{ width: 120 }} />
              <TextField size="small" label="Max Conf" value={confidenceMax} onChange={(e) => setConfidenceMax(e.target.value)} sx={{ width: 120 }} />
            </Stack>
          }
          right={
            <Stack direction="row" spacing={1}>
              <Button
                variant="outlined"
                startIcon={<Refresh />}
                onClick={() => setBulkRetryOpen(true)}
                disabled={rowSelectionModel.length === 0}
              >
                Retry Selected
              </Button>
              <Button
                variant="outlined"
                onClick={() => handleBulkSetActive(true)}
                disabled={rowSelectionModel.length === 0}
              >
                Activate
              </Button>
              <Button
                variant="outlined"
                onClick={() => handleBulkSetActive(false)}
                disabled={rowSelectionModel.length === 0}
              >
                Deactivate
              </Button>
              <Button
                color="error"
                variant="outlined"
                startIcon={<Delete />}
                onClick={() => setBulkDeleteOpen(true)}
                disabled={rowSelectionModel.length === 0}
              >
                Delete Selected ({rowSelectionModel.length})
              </Button>
            </Stack>
          }
        />

        {filteredImages.length === 0 && !loading ? (
          <EmptyState
            title="No images found"
            description="Try changing filters or add a new image."
            actionLabel="Add Image"
            onAction={openCreateDialog}
          />
        ) : (
          <DataGrid
            autoHeight
            rows={filteredImages}
            columns={columns}
            rowCount={totalRows}
            paginationMode="server"
            sortingMode="server"
            checkboxSelection
            disableRowSelectionOnClick
            rowSelectionModel={rowSelectionModel}
            onRowSelectionModelChange={(model) => setRowSelectionModel(model)}
            paginationModel={{ page, pageSize }}
            onPaginationModelChange={(m: GridPaginationModel) => {
              setPage(m.page);
              setPageSize(m.pageSize);
            }}
            sortModel={sortModel}
            onSortModelChange={setSortModel}
            pageSizeOptions={[10, 25, 50]}
          />
        )}
      </Paper>

      <Drawer anchor="right" open={!!drawerImage} onClose={() => setDrawerImage(null)}>
        <Box sx={{ width: 440, p: 3 }}>
          {drawerImage && (
            <Stack spacing={2}>
              <Typography variant="h6">{drawerImage.name}</Typography>
              <img src={getImageUrl(drawerImage.imageUrl)} alt={drawerImage.name} style={{ width: "100%", borderRadius: 8, border: "1px solid #e5e7eb" }} />
              <Typography color="text.secondary">{drawerImage.description || "No description"}</Typography>
              <Divider />
              <Typography variant="subtitle2">Default Script</Typography>
              <Typography variant="body2">{drawerImage.dialogues?.find((d) => d.isDefault)?.text || "No default script"}</Typography>
              {!drawerImage.dialogues?.find((d) => d.isDefault)?.text ? (
                <Alert severity="warning">NO_DEFAULT_SCRIPT: add a default script before mobile-ready state.</Alert>
              ) : null}
              <Divider />
              <Typography variant="subtitle2">Artifact Status</Typography>
                <StatusChip
                  status={
                    (drawerImage.talkingPhotoArtifact?.status || "queued") as
                      | "queued"
                      | "processing"
                      | "ready"
                      | "failed"
                  }
                />
              <Typography variant="caption" color="text.secondary">
                Version: {drawerImage.talkingPhotoArtifact?.version ?? "-"} • Updated:{" "}
                {drawerImage.talkingPhotoArtifact?.updatedAt
                  ? new Date(drawerImage.talkingPhotoArtifact.updatedAt).toLocaleString()
                  : "-"}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Provider: {drawerImage.talkingPhotoArtifact?.provider || "-"} • Last Duration:{" "}
                {drawerImage.talkingPhotoArtifact?.lastProcessingDurationMs != null
                  ? `${drawerImage.talkingPhotoArtifact.lastProcessingDurationMs}ms`
                  : "-"}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Correlation ID: {drawerImage.talkingPhotoArtifact?.lastCorrelationId || "-"}
              </Typography>
              {drawerImage.talkingPhotoArtifact?.errorCode ? (
                <Alert severity="warning">{drawerImage.talkingPhotoArtifact.errorCode}: {drawerImage.talkingPhotoArtifact.errorMessage}</Alert>
              ) : null}
              <Divider />
              <Typography variant="subtitle2">Preprocess Quality Gate</Typography>
              <Typography variant="body2">
                Status: {drawerImage.preprocessResult?.status || "pending"} • Eligible:{" "}
                {drawerImage.preprocessResult?.eligibleForTalkingPhoto ? "yes" : "no"}
              </Typography>
              <Typography variant="body2">
                Face detected: {drawerImage.preprocessResult?.faceDetected ? "yes" : "no"} • Confidence:{" "}
                {drawerImage.preprocessResult?.confidence != null
                  ? `${Math.round((drawerImage.preprocessResult.confidence || 0) * 100)}%`
                  : "-"}
              </Typography>
              {drawerImage.preprocessResult?.errorCode ? (
                <Alert severity="info">
                  {drawerImage.preprocessResult.errorCode}: {drawerImage.preprocessResult.errorMessage}
                </Alert>
              ) : null}
              <Stack direction="row" spacing={1}>
                <Button variant="outlined" startIcon={<Edit />} onClick={() => { setDrawerImage(null); openEditDialog(drawerImage); }}>Edit</Button>
                <Button variant="outlined" startIcon={<Refresh />} onClick={() => handleRetry(drawerImage.id)}>Retry Artifact</Button>
              </Stack>
            </Stack>
          )}
        </Box>
      </Drawer>

      <Dialog open={dialogOpen} onClose={closeDialog} fullWidth maxWidth="md">
        <DialogTitle>{isEditMode ? "Edit Image" : "Add New Image"}</DialogTitle>
        <DialogContent>
          <Stepper activeStep={uploadStep} sx={{ py: 2 }}>
            {uploadSteps.map((s) => (
              <Step key={s}><StepLabel>{s}</StepLabel></Step>
            ))}
          </Stepper>

          {uploadState !== "idle" ? (
            <Alert severity={uploadState === "failed" ? "error" : "info"} sx={{ mb: 2 }}>
              {uploadState === "uploading" && "Uploading files..."}
              {uploadState === "generating" && "Generating lip-sync artifact..."}
              {uploadState === "ready" && "Saved successfully."}
              {uploadState === "failed" && "Save failed. Please retry."}
            </Alert>
          ) : null}

          {uploadStep === 0 ? (
            <Stack spacing={2}>
              <Typography variant="subtitle2">Main Image {isEditMode ? "(optional)" : "*"}</Typography>
              <TextField type="file" inputProps={{ accept: "image/*" }} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSelectedMainFile(e.target.files?.[0] || null)} />
              <Typography variant="subtitle2">Thumbnail (optional)</Typography>
              <TextField type="file" inputProps={{ accept: "image/*" }} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSelectedThumbFile(e.target.files?.[0] || null)} />
            </Stack>
          ) : null}

          {uploadStep === 1 ? (
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField fullWidth label="Name *" value={uploadName} onChange={(e) => setUploadName(e.target.value)} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth multiline minRows={3} label="Description" value={uploadDescription} onChange={(e) => setUploadDescription(e.target.value)} />
              </Grid>
            </Grid>
          ) : null}

          {uploadStep === 2 ? (
            <TextField fullWidth multiline minRows={5} label="Default Script *" value={uploadScript} onChange={(e) => setUploadScript(e.target.value)} />
          ) : null}

          {uploadStep === 3 ? (
            <Stack spacing={1}>
              <Typography><strong>Name:</strong> {uploadName}</Typography>
              <Typography><strong>Description:</strong> {uploadDescription || "-"}</Typography>
              <Typography><strong>Script:</strong> {uploadScript || "-"}</Typography>
              <Typography><strong>Main File:</strong> {selectedMainFile?.name || (isEditMode ? "Keep existing" : "Not selected")}</Typography>
              <Typography><strong>Thumbnail:</strong> {selectedThumbFile?.name || "Not selected"}</Typography>
            </Stack>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancel</Button>
          <Button disabled={uploadStep === 0} onClick={() => setUploadStep((s) => s - 1)}>Back</Button>
          {uploadStep < uploadSteps.length - 1 ? (
            <Button variant="contained" onClick={() => setUploadStep((s) => s + 1)} disabled={!validateStep()}>
              Next
            </Button>
          ) : (
            <Button variant="contained" startIcon={<Upload />} onClick={handleSubmitUpload} disabled={!validateStep() || uploadState === "uploading" || uploadState === "generating"}>
              {isEditMode ? "Update" : "Create"}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete Image"
        description={`Delete ${deleteTarget?.name}? This cannot be undone.`}
        confirmLabel="Delete"
        onClose={() => setDeleteTarget(null)}
        onConfirm={async () => {
          if (!deleteTarget) return;
          await handleDelete(deleteTarget.id);
          setDeleteTarget(null);
        }}
      />

      <ConfirmDialog
        open={bulkRetryOpen}
        title="Retry Artifact Generation"
        description={`Retry artifact generation for ${rowSelectionModel.length} selected images?`}
        confirmLabel="Retry"
        onClose={() => setBulkRetryOpen(false)}
        onConfirm={handleBulkRetry}
      />

      <ConfirmDialog
        open={bulkDeleteOpen}
        title="Delete Selected Images"
        description={`Delete ${rowSelectionModel.length} selected images? This cannot be undone.`}
        confirmLabel="Delete All"
        onClose={() => setBulkDeleteOpen(false)}
        onConfirm={handleBulkDelete}
      />
    </Box>
  );
}

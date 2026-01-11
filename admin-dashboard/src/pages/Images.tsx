import React, { useEffect, useState } from "react";
import { 
  Typography, 
  Paper, 
  Box, 
  Grid, 
  Card, 
  CardMedia, 
  CardContent, 
  CardActions,
  CircularProgress,
  Alert,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Tooltip,
  Divider,
  Stack
} from "@mui/material";
import { 
  Add as AddIcon, 
  Close as CloseIcon, 
  CloudUpload as UploadIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  Photo as PhotoIcon,
  Description as ScriptIcon,
  Info as InfoIcon
} from "@mui/icons-material";
import { ImageService, Image } from "../services/imageService";

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:4000';

export default function ImagesPage() {
  const [images, setImages] = useState<Image[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [selectedImage, setSelectedImage] = useState<Image | null>(null);
  
  // Upload form state
  const [isEditMode, setIsEditMode] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [uploadName, setUploadName] = useState("");
  const [uploadDescription, setUploadDescription] = useState("");
  const [uploadScript, setUploadScript] = useState("");
  const [selectedMainFile, setSelectedMainFile] = useState<File | null>(null);
  const [selectedThumbFile, setSelectedThumbFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  const fetchImages = async () => {
    setLoading(true);
    try {
      const response = await ImageService.getAllImages();
      setImages(response.data);
    } catch (err) {
      console.error("Error fetching images:", err);
      setError("Failed to load images from the database.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchImages();
  }, []);

  const handleOpenUpload = (image?: Image) => {
    if (image) {
      setIsEditMode(true);
      setEditId(image.id);
      setUploadName(image.name);
      setUploadDescription(image.description || "");
      setUploadScript(image.dialogues?.find(d => d.isDefault)?.text || "");
    } else {
      setIsEditMode(false);
      setEditId(null);
      setUploadName("");
      setUploadDescription("");
      setUploadScript("");
    }
    setSelectedMainFile(null);
    setSelectedThumbFile(null);
    setUploadDialogOpen(true);
  };

  const handleCloseUpload = () => {
    setUploadDialogOpen(false);
  };

  const handleUpload = async () => {
    if (!uploadName) return;
    if (!isEditMode && !selectedMainFile) return;

    setUploading(true);
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
      handleCloseUpload();
      fetchImages(); // Refresh list
    } catch (err) {
      console.error("Operation failed:", err);
      alert("Operation failed. Please try again.");
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm("Are you sure you want to delete this product?")) return;
    try {
      await ImageService.deleteImage(id);
      fetchImages();
    } catch (err) {
      console.error("Delete failed:", err);
      alert("Failed to delete image.");
    }
  };

  const handleViewDetails = (image: Image) => {
    setSelectedImage(image);
    setViewDialogOpen(true);
  };

  const getImageUrl = (path: string) => {
    if (!path) return '';
    return path.startsWith('/') ? `${API_URL}${path}` : path;
  };

  if (loading && images.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom>
            Dataset Images
          </Typography>
          <Typography variant="body1">
            These are the images currently in your dataset.
          </Typography>
        </Box>
        <Button 
          variant="contained" 
          startIcon={<AddIcon />} 
          onClick={() => handleOpenUpload()}
          size="large"
        >
          Add Image
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      
      <Grid container spacing={3}>
        {images.length > 0 ? (
          images.map((image) => (
            <Grid item xs={12} sm={6} md={4} key={image.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardMedia
                  component="img"
                  height="200"
                  image={getImageUrl(image.imageUrl)}
                  alt={image.name}
                  sx={{ objectFit: 'contain', bgcolor: 'grey.100', p: 1 }}
                />
                <CardContent sx={{ flexGrow: 1 }}>
                  <Typography gutterBottom variant="h6" component="div" noWrap>
                    {image.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ 
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                    overflow: 'hidden',
                    height: '3em'
                  }}>
                    {image.description || "No description provided."}
                  </Typography>
                </CardContent>
                <Divider />
                <CardActions sx={{ justifyContent: 'space-between', px: 2 }}>
                  <Box>
                    <Tooltip title="View Details">
                      <IconButton size="small" onClick={() => handleViewDetails(image)}>
                        <ViewIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Edit">
                      <IconButton size="small" onClick={() => handleOpenUpload(image)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Box>
                  <Tooltip title="Delete">
                    <IconButton size="small" color="error" onClick={() => handleDelete(image.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </CardActions>
              </Card>
            </Grid>
          ))
        ) : (
          <Grid item xs={12}>
            <Paper sx={{ p: 5, textAlign: 'center', bgcolor: 'grey.50', border: '1px dashed grey.400' }}>
              <Typography variant="h6" color="text.secondary">No images found in the dataset.</Typography>
              <Button 
                variant="outlined" 
                startIcon={<AddIcon />} 
                onClick={() => handleOpenUpload()}
                sx={{ mt: 2 }}
              >
                Upload your first image
              </Button>
            </Paper>
          </Grid>
        )}
      </Grid>

      {/* Upload/Edit Dialog */}
      <Dialog open={uploadDialogOpen} onClose={handleCloseUpload} maxWidth="md" fullWidth>
        <DialogTitle>
          {isEditMode ? "Edit Product" : "Add New Product"}
          <IconButton
            onClick={handleCloseUpload}
            sx={{ position: 'absolute', right: 8, top: 8 }}
          >
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={3}>
            {/* Image Uploads */}
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" gutterBottom>Main Product Image *</Typography>
              <Box sx={{ mb: 2 }}>
                <input
                  accept="image/*"
                  style={{ display: 'none' }}
                  id="main-file-input"
                  type="file"
                  onChange={(e) => e.target.files && setSelectedMainFile(e.target.files[0])}
                />
                <label htmlFor="main-file-input">
                  <Paper 
                    variant="outlined" 
                    sx={{ 
                      p: 2, 
                      textAlign: 'center', 
                      cursor: 'pointer',
                      borderStyle: 'dashed',
                      height: 120,
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'center',
                      alignItems: 'center',
                      bgcolor: selectedMainFile ? 'primary.50' : 'transparent',
                      '&:hover': { bgcolor: 'action.hover' }
                    }}
                  >
                    <PhotoIcon color={selectedMainFile ? "primary" : "action"} />
                    <Typography variant="caption" sx={{ mt: 1, fontWeight: selectedMainFile ? 'bold' : 'normal' }}>
                      {selectedMainFile ? selectedMainFile.name : (isEditMode ? "Change Main Image" : "Upload Main Image")}
                    </Typography>
                  </Paper>
                </label>
              </Box>

              <Typography variant="subtitle2" gutterBottom>Marker/Thumbnail Image (Optional)</Typography>
              <Box>
                <input
                  accept="image/*"
                  style={{ display: 'none' }}
                  id="thumb-file-input"
                  type="file"
                  onChange={(e) => e.target.files && setSelectedThumbFile(e.target.files[0])}
                />
                <label htmlFor="thumb-file-input">
                  <Paper 
                    variant="outlined" 
                    sx={{ 
                      p: 2, 
                      textAlign: 'center', 
                      cursor: 'pointer',
                      borderStyle: 'dashed',
                      height: 120,
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'center',
                      alignItems: 'center',
                      bgcolor: selectedThumbFile ? 'secondary.50' : 'transparent',
                      '&:hover': { bgcolor: 'action.hover' }
                    }}
                  >
                    <PhotoIcon color={selectedThumbFile ? "secondary" : "action"} />
                    <Typography variant="caption" sx={{ mt: 1, fontWeight: selectedThumbFile ? 'bold' : 'normal' }}>
                      {selectedThumbFile ? selectedThumbFile.name : (isEditMode ? "Change Marker Image" : "Upload Marker Image")}
                    </Typography>
                  </Paper>
                </label>
              </Box>
            </Grid>

            {/* Form Fields */}
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Product Name *"
                value={uploadName}
                onChange={(e) => setUploadName(e.target.value)}
                sx={{ mb: 2 }}
                size="small"
              />
              <TextField
                fullWidth
                label="Product Description"
                multiline
                rows={3}
                value={uploadDescription}
                onChange={(e) => setUploadDescription(e.target.value)}
                sx={{ mb: 2 }}
                size="small"
              />
              <TextField
                fullWidth
                label="Initial Script / Dialogue"
                multiline
                rows={4}
                value={uploadScript}
                onChange={(e) => setUploadScript(e.target.value)}
                placeholder="Hello! I'm your product assistant. How can I help you today?"
                sx={{ mb: 2 }}
                size="small"
                InputProps={{
                  startAdornment: <ScriptIcon sx={{ mr: 1, color: 'action.active' }} fontSize="small" />,
                }}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={handleCloseUpload} color="inherit">Cancel</Button>
          <Button 
            variant="contained" 
            onClick={handleUpload} 
            disabled={!uploadName || (!isEditMode && !selectedMainFile) || uploading}
            startIcon={uploading ? <CircularProgress size={20} /> : (isEditMode ? <EditIcon /> : <UploadIcon />)}
          >
            {uploading ? "Processing..." : (isEditMode ? "Update Product" : "Add Product")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* View Details Dialog */}
      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          Product Details
          <IconButton
            onClick={() => setViewDialogOpen(false)}
            sx={{ position: 'absolute', right: 8, top: 8 }}
          >
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent dividers>
          {selectedImage && (
            <Grid container spacing={4}>
              <Grid item xs={12} md={5}>
                <Typography variant="overline" color="text.secondary">Main Image</Typography>
                <Paper variant="outlined" sx={{ p: 1, mb: 2 }}>
                  <img 
                    src={getImageUrl(selectedImage.imageUrl)} 
                    alt="Main" 
                    style={{ width: '100%', borderRadius: 4, display: 'block' }} 
                  />
                </Paper>
                
                <Typography variant="overline" color="text.secondary">Marker/Thumbnail Image</Typography>
                <Paper variant="outlined" sx={{ p: 1 }}>
                  <img 
                    src={getImageUrl(selectedImage.thumbnailUrl || selectedImage.imageUrl)} 
                    alt="Thumbnail" 
                    style={{ width: '100%', borderRadius: 4, display: 'block' }} 
                  />
                </Paper>
              </Grid>
              
              <Grid item xs={12} md={7}>
                <Box mb={3}>
                  <Typography variant="h5" fontWeight="bold">{selectedImage.name}</Typography>
                  <Typography variant="body1" color="text.secondary" sx={{ mt: 1 }}>
                    {selectedImage.description || "No description provided."}
                  </Typography>
                </Box>
                
                <Divider sx={{ mb: 3 }} />
                
                <Box mb={3}>
                  <Box display="flex" alignItems="center" mb={1}>
                    <ScriptIcon color="primary" fontSize="small" sx={{ mr: 1 }} />
                    <Typography variant="subtitle1" fontWeight="bold">Current Script</Typography>
                  </Box>
                  <Paper sx={{ p: 2, bgcolor: 'grey.50', borderLeft: '4px solid #1976d2' }}>
                    <Typography variant="body1" sx={{ fontStyle: 'italic' }}>
                      "{selectedImage.dialogues?.find(d => d.isDefault)?.text || "No script defined yet."}"
                    </Typography>
                  </Paper>
                </Box>
                
                <Box display="flex" gap={2}>
                  <Button 
                    variant="outlined" 
                    startIcon={<EditIcon />} 
                    onClick={() => {
                      setViewDialogOpen(false);
                      handleOpenUpload(selectedImage);
                    }}
                  >
                    Edit Product
                  </Button>
                  <Button 
                    variant="text" 
                    color="error" 
                    startIcon={<DeleteIcon />}
                    onClick={() => {
                      setViewDialogOpen(false);
                      handleDelete(selectedImage.id);
                    }}
                  >
                    Delete
                  </Button>
                </Box>
              </Grid>
            </Grid>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

import React, { useState, useEffect } from "react";
import {
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CardMedia,
  Grid,
  Typography,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
} from "@mui/material";
import { Add as AddIcon } from "@mui/icons-material";
import { API_ORIGIN } from "../services/api";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import {
  fetchImages,
  createImage,
  updateImage,
  deleteImage,
} from "../store/slices/imageSlice";
import type { Image as ImageModel } from "../store/slices/imageSlice";
import { MultiImageUploadDialog } from "../components/MultiImageUploadDialog";
import type { ImageSet } from "../services/multiImageService";

export default function Images() {
  const dispatch = useAppDispatch();
  const {
    images,
    loading: listLoading,
    error: listError,
  } = useAppSelector((s) => s.images);

  const [open, setOpen] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    image: null as File | null,
  });
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  // Edit dialog state
  const [editOpen, setEditOpen] = useState(false);
  const [editData, setEditData] = useState<{
    id: string;
    name: string;
    description: string;
  }>({
    id: "",
    name: "",
    description: "",
  });

  // Multi-image upload dialog state
  const [multiImageOpen, setMultiImageOpen] = useState(false);

  useEffect(() => {
    dispatch(fetchImages());
  }, [dispatch]);

  // Debug: Log image URLs
  useEffect(() => {
    if (images.length > 0) {
      console.log("Images loaded:", images.length);
      images.forEach((image, index) => {
        console.log(`Image ${index + 1}:`, {
          name: image.name,
          imageUrl: image.imageUrl,
          fullUrl: `${API_ORIGIN}${image.imageUrl}`,
          API_ORIGIN: API_ORIGIN,
        });
      });
    }
  }, [images]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setUploading(true);
    setError(null);

    try {
      const formDataToSend = new FormData();
      formDataToSend.append("name", formData.name);
      formDataToSend.append("description", formData.description);
      if (formData.image) {
        formDataToSend.append("image", formData.image);
      }

      await dispatch(createImage(formDataToSend)).unwrap();

      setOpen(false);
      setFormData({ name: "", description: "", image: null });
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
        setPreviewUrl(null);
      }
    } catch (err) {
      setError("Failed to upload image");
    } finally {
      setUploading(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setFormData({ ...formData, image: file });
      const url = URL.createObjectURL(file);
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      setPreviewUrl(url);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await dispatch(deleteImage(id)).unwrap();
    } catch (err) {
      setError("Failed to delete image");
    }
  };

  const openEdit = (image: ImageModel) => {
    setEditData({
      id: image.id,
      name: image.name,
      description: image.description || "",
    });
    setEditOpen(true);
  };

  const handleEditSave = async () => {
    try {
      await dispatch(
        updateImage({
          id: editData.id,
          data: { name: editData.name, description: editData.description },
        })
      ).unwrap();
      setEditOpen(false);
    } catch (err) {
      setError("Failed to update image");
    }
  };

  const handleMultiImageSave = async (imageSet: ImageSet): Promise<void> => {
    try {
      console.log("Multi-image save completed, refreshing images...");
      // Refresh the images list to show the new uploads
      await dispatch(fetchImages());
      setMultiImageOpen(false);
      console.log("Images refreshed successfully");
    } catch (err) {
      console.error("Failed to refresh images:", err);
      setError("Failed to save multi-image set");
    }
  };

  return (
    <Box>
      <Box sx={{ display: "flex", justifyContent: "space-between", mb: 3 }}>
        <Typography variant="h4">Images</Typography>
        <Box sx={{ display: "flex", gap: 2 }}>
          <Button
            variant="outlined"
            startIcon={<AddIcon />}
            onClick={() => setMultiImageOpen(true)}
          >
            Upload Multi-Images
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpen(true)}
          >
            Upload Single Image
          </Button>
        </Box>
      </Box>

      {(error || listError) && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error || listError}
        </Alert>
      )}

      <Grid container spacing={3}>
        {images.map((image) => (
          <Grid item xs={12} sm={6} md={4} key={image.id}>
            <Card>
              <CardMedia
                component="img"
                height="200"
                image={`${API_ORIGIN}${image.imageUrl}`}
                alt={image.name}
                onError={(e) => {
                  console.error("Image load error:", e);
                  console.error(
                    "Failed to load image:",
                    `${API_ORIGIN}${image.imageUrl}`
                  );
                }}
                onLoad={() => {
                  console.log(
                    "Image loaded successfully:",
                    `${API_ORIGIN}${image.imageUrl}`
                  );
                }}
              />
              <CardContent>
                <Typography gutterBottom variant="h6" component="div">
                  {image.name}
                </Typography>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ minHeight: 48 }}
                >
                  {image.description}
                </Typography>
                {image.isMultiImage && (
                  <Typography
                    variant="caption"
                    display="block"
                    sx={{ mt: 1, color: "primary.main" }}
                  >
                    📦 Multi-Image Set: {image.objectName} ({image.imageType})
                  </Typography>
                )}
                <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                  Created: {new Date(image.createdAt).toLocaleDateString()}
                </Typography>
              </CardContent>
              <CardActions sx={{ justifyContent: "flex-end" }}>
                {!image.isMultiImage && (
                  <>
                    <Button size="small" onClick={() => openEdit(image)}>
                      Edit
                    </Button>
                    <Button
                      size="small"
                      color="error"
                      onClick={() => handleDelete(image.id)}
                    >
                      Delete
                    </Button>
                  </>
                )}
                {image.isMultiImage && (
                  <Typography variant="caption" color="text.secondary">
                    Managed by Multi-Image System
                  </Typography>
                )}
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Upload New Image</DialogTitle>
        <form onSubmit={handleSubmit}>
          <DialogContent>
            <TextField
              autoFocus
              margin="dense"
              label="Image Name"
              fullWidth
              variant="outlined"
              value={formData.name}
              onChange={(e) =>
                setFormData({ ...formData, name: e.target.value })
              }
              required
            />
            <TextField
              margin="dense"
              label="Description"
              fullWidth
              multiline
              rows={3}
              variant="outlined"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
            />
            <Box sx={{ mt: 2 }}>
              <input
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                required
              />
            </Box>
            {previewUrl && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="subtitle2">Preview</Typography>
                <Box
                  component="img"
                  src={previewUrl}
                  alt="preview"
                  sx={{
                    maxWidth: "100%",
                    maxHeight: 240,
                    borderRadius: 1,
                    border: "1px solid #eee",
                  }}
                />
              </Box>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit" disabled={uploading}>
              {uploading ? "Uploading..." : "Upload"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Edit Image Dialog */}
      <Dialog
        open={editOpen}
        onClose={() => setEditOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Edit Image</DialogTitle>
        <DialogContent>
          <TextField
            margin="dense"
            label="Image Name"
            fullWidth
            variant="outlined"
            value={editData.name}
            onChange={(e) => setEditData({ ...editData, name: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Description"
            fullWidth
            multiline
            rows={3}
            variant="outlined"
            value={editData.description}
            onChange={(e) =>
              setEditData({ ...editData, description: e.target.value })
            }
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>Cancel</Button>
          <Button onClick={handleEditSave} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Multi-Image Upload Dialog */}
      <MultiImageUploadDialog
        open={multiImageOpen}
        onClose={() => setMultiImageOpen(false)}
        onSave={handleMultiImageSave}
      />
    </Box>
  );
}

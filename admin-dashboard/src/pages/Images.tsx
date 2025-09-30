import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Button,
  Grid,
  Card,
  CardContent,
  CardMedia,
  CardActions,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Switch,
  FormControlLabel,
} from "@mui/material";
import {
  Add,
  Edit,
  Delete,
  Visibility,
  VisibilityOff,
} from "@mui/icons-material";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store/store";
import {
  fetchImages,
  updateImage,
  deleteImage,
} from "../store/slices/imageSlice";
import { ImageUploadDialog } from "../components/ImageUploadDialog";

const Images: React.FC = () => {
  const dispatch = useDispatch();
  const { images, loading } = useSelector((state: RootState) => state.images);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [selectedImage, setSelectedImage] = useState<any>(null);
  const [editData, setEditData] = useState({
    name: "",
    description: "",
    isActive: true,
  });

  useEffect(() => {
    dispatch(fetchImages());
  }, [dispatch]);

  const handleEdit = (image: any) => {
    setSelectedImage(image);
    setEditData({
      name: image.name,
      description: image.description || "",
      isActive: image.isActive,
    });
    setEditDialogOpen(true);
  };

  const handleUpdate = () => {
    if (selectedImage) {
      dispatch(updateImage({ id: selectedImage.id, data: editData }));
      setEditDialogOpen(false);
    }
  };

  const handleDelete = (id: string) => {
    if (window.confirm("Are you sure you want to delete this image?")) {
      dispatch(deleteImage(id));
    }
  };

  const handleToggleActive = (image: any) => {
    dispatch(
      updateImage({ id: image.id, data: { isActive: !image.isActive } })
    );
  };

  if (loading) {
    return <Typography>Loading...</Typography>;
  }

  return (
    <Box>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mb={3}
      >
        <Typography variant="h4">Images</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => setUploadDialogOpen(true)}
        >
          Add Image
        </Button>
      </Box>

      <Grid container spacing={3}>
        {images.map((image) => (
          <Grid item xs={12} sm={6} md={4} key={image.id}>
            <Card>
              <CardMedia
                component="img"
                height="200"
                image={image.imageUrl}
                alt={image.name}
              />
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  {image.name}
                </Typography>
                <Typography variant="body2" color="text.secondary" paragraph>
                  {image.description}
                </Typography>
                <Box display="flex" gap={1} mb={2}>
                  <Chip
                    label={image.isActive ? "Active" : "Inactive"}
                    color={image.isActive ? "success" : "default"}
                    size="small"
                  />
                  <Chip
                    label={`${image.dialogues.length} dialogues`}
                    variant="outlined"
                    size="small"
                  />
                </Box>
              </CardContent>
              <CardActions>
                <IconButton
                  onClick={() => handleToggleActive(image)}
                  color={image.isActive ? "primary" : "default"}
                >
                  {image.isActive ? <Visibility /> : <VisibilityOff />}
                </IconButton>
                <IconButton onClick={() => handleEdit(image)}>
                  <Edit />
                </IconButton>
                <IconButton
                  onClick={() => handleDelete(image.id)}
                  color="error"
                >
                  <Delete />
                </IconButton>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      <ImageUploadDialog
        open={uploadDialogOpen}
        onClose={() => setUploadDialogOpen(false)}
      />

      <Dialog
        open={editDialogOpen}
        onClose={() => setEditDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Edit Image</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Name"
            fullWidth
            variant="outlined"
            value={editData.name}
            onChange={(e) => setEditData({ ...editData, name: e.target.value })}
            sx={{ mb: 2 }}
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
            sx={{ mb: 2 }}
          />
          <FormControlLabel
            control={
              <Switch
                checked={editData.isActive}
                onChange={(e) =>
                  setEditData({ ...editData, isActive: e.target.checked })
                }
              />
            }
            label="Active"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleUpdate} variant="contained">
            Update
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Images;

import React from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Grid,
  Box,
  Typography,
} from "@mui/material";

export interface GalleryImage {
  id: string;
  name: string;
  imageUrl: string;
  imageType?: string;
}

interface ImageGalleryModalProps {
  open: boolean;
  images: GalleryImage[];
  onClose: () => void;
  objectName?: string;
}

export const ImageGalleryModal: React.FC<ImageGalleryModalProps> = ({
  open,
  images,
  onClose,
  objectName,
}) => {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Preview: {objectName ?? "Images"}</DialogTitle>
      <DialogContent>
        <Grid container spacing={2}>
          {images.map((img) => (
            <Grid item xs={12} sm={6} md={4} key={img.id}>
              <Box
                component="img"
                src={img.imageUrl}
                alt={img.name}
                sx={{
                  width: "100%",
                  maxHeight: 200,
                  borderRadius: 1,
                  mb: 1,
                  objectFit: "cover",
                }}
              />
              <Typography variant="body2" color="text.secondary">
                {img.name}
                {img.imageType ? ` (${img.imageType})` : ""}
              </Typography>
            </Grid>
          ))}
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="contained">
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

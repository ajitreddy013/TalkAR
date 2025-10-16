import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  Card,
  CardContent,
  IconButton,
  Alert,
  LinearProgress,
  Grid,
  Paper,
} from "@mui/material";
import { CloudUpload, Delete, CheckCircle, Error } from "@mui/icons-material";
import { useDropzone } from "react-dropzone";
import {
  MultiImageService,
  MultiImageUpload,
  ImageSet as ServerImageSet,
} from "../services/multiImageService";

interface MultiImageUploadDialogProps {
  open: boolean;
  onClose: () => void;
  onSave: (imageSet: ServerImageSet) => Promise<void>;
}

interface LocalImageSet {
  objectName: string;
  description: string;
  images: {
    type:
      | "front"
      | "left_angle"
      | "right_angle"
      | "bright"
      | "dim"
      | "close"
      | "far";
    file: File;
    description: string;
    required: boolean;
  }[];
}

const REQUIRED_IMAGE_TYPES = [
  {
    type: "front",
    label: "Front View",
    description: "Straight-on, primary reference",
    required: true,
  },
  {
    type: "left_angle",
    label: "Left Angle",
    description: "15-30° left view",
    required: true,
  },
  {
    type: "right_angle",
    label: "Right Angle",
    description: "15-30° right view",
    required: true,
  },
  {
    type: "bright",
    label: "Bright Lighting",
    description: "Outdoor/sunny conditions",
    required: true,
  },
  {
    type: "dim",
    label: "Dim Lighting",
    description: "Indoor/low-light conditions",
    required: true,
  },
  {
    type: "close",
    label: "Close Distance",
    description: "1-2 feet away (optional)",
    required: false,
  },
  {
    type: "far",
    label: "Far Distance",
    description: "4-6 feet away (optional)",
    required: false,
  },
];

export const MultiImageUploadDialog: React.FC<MultiImageUploadDialogProps> = ({
  open,
  onClose,
  onSave,
}) => {
  const [objectName, setObjectName] = useState("");
  const [description, setDescription] = useState("");
  const [images, setImages] = useState<{ [key: string]: File }>({});
  const [imageUrls, setImageUrls] = useState<{ [key: string]: string }>({});
  const [uploading, setUploading] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);

  // Cleanup object URLs on unmount
  useEffect(() => {
    return () => {
      Object.values(imageUrls).forEach((url) => {
        URL.revokeObjectURL(url);
      });
    };
  }, [imageUrls]);

  const handleDrop = (acceptedFiles: File[], imageType: string) => {
    if (acceptedFiles.length > 0) {
      const file = acceptedFiles[0];

      // Validate file
      if (!file.type.startsWith("image/")) {
        setErrors((prev) => [
          ...prev,
          `${imageType}: Only image files are allowed`,
        ]);
        return;
      }

      if (file.size > 10 * 1024 * 1024) {
        // 10MB limit
        setErrors((prev) => [
          ...prev,
          `${imageType}: File size must be less than 10MB`,
        ]);
        return;
      }

      // Revoke old URL if exists
      if (imageUrls[imageType]) {
        URL.revokeObjectURL(imageUrls[imageType]);
      }

      // Create new object URL
      const objectUrl = URL.createObjectURL(file);

      setImages((prev) => ({ ...prev, [imageType]: file }));
      setImageUrls((prev) => ({ ...prev, [imageType]: objectUrl }));
      setErrors((prev) => prev.filter((error) => !error.startsWith(imageType)));
    }
  };

  const handleRemove = (imageType: string) => {
    // Revoke object URL to prevent memory leak
    if (imageUrls[imageType]) {
      URL.revokeObjectURL(imageUrls[imageType]);
    }

    setImages((prev) => {
      const newImages = { ...prev };
      delete newImages[imageType];
      return newImages;
    });
    setImageUrls((prev) => {
      const newUrls = { ...prev };
      delete newUrls[imageType];
      return newUrls;
    });
  };

  const handleSave = async () => {
    setErrors([]);

    // Validate required fields
    if (!objectName.trim()) {
      setErrors(["Object name is required"]);
      return;
    }

    // Validate required images
    const missingRequired = REQUIRED_IMAGE_TYPES.filter(
      (item) => item.required && !images[item.type]
    ).map((item) => item.label);

    if (missingRequired.length > 0) {
      setErrors([`Missing required images: ${missingRequired.join(", ")}`]);
      return;
    }

    setUploading(true);

    try {
      const multiImageUpload: MultiImageUpload = {
        objectName: objectName.trim(),
        description: description.trim(),
        images: Object.entries(images).map(([type, file]) => ({
          type: type as any,
          file,
          description:
            REQUIRED_IMAGE_TYPES.find((item) => item.type === type)
              ?.description || "",
          required:
            REQUIRED_IMAGE_TYPES.find((item) => item.type === type)?.required ||
            false,
        })),
      };

      // Debug: print the API URL used by the client
      try {
        // eslint-disable-next-line no-console
        console.log(
          "Uploading to:",
          (window as any).API_BASE_URL || process.env.REACT_APP_API_URL
        );
      } catch (e) {
        // ignore
      }

      // Upload to backend
      const result = await MultiImageService.uploadMultiImages(
        multiImageUpload
      );

      // Call onSave with the result
      await onSave(result.imageSet);

      // Cleanup object URLs before reset
      Object.values(imageUrls).forEach((url) => {
        URL.revokeObjectURL(url);
      });

      // Reset form
      setObjectName("");
      setDescription("");
      setImages({});
      setImageUrls({});
      setErrors([]);
    } catch (error: any) {
      console.error("Upload failed:", error);
      setErrors([
        `Failed to save image set: ${
          error.response?.data?.error || error.message
        }`,
      ]);
    } finally {
      setUploading(false);
    }
  };

  const getImagePreview = (imageType: string) => {
    const url = imageUrls[imageType];
    if (!url) return null;

    return (
      <Box
        component="img"
        src={url}
        alt={`${imageType} preview`}
        sx={{
          width: "100%",
          height: 120,
          objectFit: "cover",
          borderRadius: 1,
        }}
      />
    );
  };

  const isRequiredImageMissing = (imageType: string) => {
    const required = REQUIRED_IMAGE_TYPES.find(
      (item) => item.type === imageType
    )?.required;
    return required && !images[imageType];
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        Upload Multiple Images for AR Recognition
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          Upload 5-7 images per object for reliable AR recognition across
          different conditions
        </Typography>
      </DialogTitle>

      <DialogContent>
        <Box sx={{ mb: 3 }}>
          <TextField
            fullWidth
            label="Object Name"
            value={objectName}
            onChange={(e) => setObjectName(e.target.value)}
            placeholder="e.g., TalkAR Logo, Product A, etc."
            required
          />
        </Box>

        <Box sx={{ mb: 3 }}>
          <TextField
            fullWidth
            label="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Brief description of the object"
            multiline
            rows={2}
          />
        </Box>

        {errors.length > 0 && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {errors.map((error, index) => (
              <div key={index}>{error}</div>
            ))}
          </Alert>
        )}

        <Typography variant="h6" sx={{ mb: 2 }}>
          Required Images (5)
        </Typography>

        <Grid container spacing={2} sx={{ mb: 3 }}>
          {REQUIRED_IMAGE_TYPES.filter((item) => item.required).map((item) => (
            <Grid item xs={12} sm={6} md={4} key={item.type}>
              <Card
                variant={
                  isRequiredImageMissing(item.type) ? "outlined" : "elevation"
                }
                sx={{
                  border: isRequiredImageMissing(item.type)
                    ? "2px solid #f44336"
                    : "none",
                  height: "100%",
                }}
              >
                <CardContent>
                  <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
                    <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
                      {item.label}
                    </Typography>
                    {images[item.type] && (
                      <CheckCircle color="success" fontSize="small" />
                    )}
                    {isRequiredImageMissing(item.type) && (
                      <Error color="error" fontSize="small" />
                    )}
                  </Box>

                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ mb: 2 }}
                  >
                    {item.description}
                  </Typography>

                  {images[item.type] ? (
                    <Box>
                      {getImagePreview(item.type)}
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                          mt: 1,
                        }}
                      >
                        <Typography variant="caption" color="text.secondary">
                          {images[item.type].name}
                        </Typography>
                        <IconButton
                          size="small"
                          onClick={() => handleRemove(item.type)}
                          color="error"
                        >
                          <Delete fontSize="small" />
                        </IconButton>
                      </Box>
                    </Box>
                  ) : (
                    <ImageDropzone
                      onDrop={(files) => handleDrop(files, item.type)}
                      imageType={item.type}
                    />
                  )}
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        <Typography variant="h6" sx={{ mb: 2 }}>
          Optional Images (2)
        </Typography>

        <Grid container spacing={2}>
          {REQUIRED_IMAGE_TYPES.filter((item) => !item.required).map((item) => (
            <Grid item xs={12} sm={6} md={4} key={item.type}>
              <Card variant="outlined" sx={{ height: "100%" }}>
                <CardContent>
                  <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
                    <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
                      {item.label}
                    </Typography>
                    {images[item.type] && (
                      <CheckCircle color="success" fontSize="small" />
                    )}
                  </Box>

                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ mb: 2 }}
                  >
                    {item.description}
                  </Typography>

                  {images[item.type] ? (
                    <Box>
                      {getImagePreview(item.type)}
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                          mt: 1,
                        }}
                      >
                        <Typography variant="caption" color="text.secondary">
                          {images[item.type].name}
                        </Typography>
                        <IconButton
                          size="small"
                          onClick={() => handleRemove(item.type)}
                          color="error"
                        >
                          <Delete fontSize="small" />
                        </IconButton>
                      </Box>
                    </Box>
                  ) : (
                    <ImageDropzone
                      onDrop={(files) => handleDrop(files, item.type)}
                      imageType={item.type}
                    />
                  )}
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        <Box sx={{ mt: 3, p: 2, bgcolor: "info.light", borderRadius: 1 }}>
          <Typography variant="body2" color="info.contrastText">
            <strong>Tip:</strong> Upload high-quality images with good contrast
            and multiple distinctive features. The more varied your images
            (angles, lighting, distances), the better the AR recognition will
            work.
          </Typography>
        </Box>
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} disabled={uploading}>
          Cancel
        </Button>
        <Button
          onClick={handleSave}
          variant="contained"
          disabled={uploading || !objectName.trim()}
        >
          {uploading ? "Saving..." : "Save Image Set"}
        </Button>
      </DialogActions>

      {uploading && <LinearProgress />}
    </Dialog>
  );
};

interface ImageDropzoneProps {
  onDrop: (files: File[]) => void;
  imageType: string;
}

const ImageDropzone: React.FC<ImageDropzoneProps> = ({ onDrop, imageType }) => {
  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "image/*": [".jpeg", ".jpg", ".png", ".gif", ".bmp"],
    },
    multiple: false,
    maxSize: 10 * 1024 * 1024, // 10MB
  });

  return (
    <Paper
      {...getRootProps()}
      sx={{
        p: 2,
        textAlign: "center",
        cursor: "pointer",
        border: "2px dashed #ccc",
        bgcolor: isDragActive ? "action.hover" : "background.paper",
        "&:hover": {
          bgcolor: "action.hover",
        },
      }}
    >
      <input {...getInputProps()} />
      <CloudUpload sx={{ fontSize: 40, color: "text.secondary", mb: 1 }} />
      <Typography variant="body2" color="text.secondary">
        {isDragActive ? "Drop image here" : "Click or drag image here"}
      </Typography>
      <Typography variant="caption" color="text.secondary">
        Max 10MB, JPG/PNG/GIF
      </Typography>
    </Paper>
  );
};

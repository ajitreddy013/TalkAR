import React, { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Button,
  Grid,
  Card,
  CardContent,
  CardActions,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
} from "@mui/material";
import { Add, Edit, Delete, Language } from "@mui/icons-material";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store/store";
import { fetchImages } from "../store/slices/imageSlice";
import { DialogueDialog } from "../components/DialogueDialog";

const Dialogues: React.FC = () => {
  const dispatch = useDispatch();
  const { images } = useSelector((state: RootState) => state.images);
  const [dialogueDialogOpen, setDialogueDialogOpen] = useState(false);
  const [selectedImage, setSelectedImage] = useState<any>(null);
  const [selectedDialogue, setSelectedDialogue] = useState<any>(null);

  useEffect(() => {
    dispatch(fetchImages());
  }, [dispatch]);

  const handleAddDialogue = (image: any) => {
    setSelectedImage(image);
    setSelectedDialogue(null);
    setDialogueDialogOpen(true);
  };

  const handleEditDialogue = (image: any, dialogue: any) => {
    setSelectedImage(image);
    setSelectedDialogue(dialogue);
    setDialogueDialogOpen(true);
  };

  const languages = [
    { code: "en", name: "English" },
    { code: "es", name: "Spanish" },
    { code: "fr", name: "French" },
    { code: "de", name: "German" },
    { code: "it", name: "Italian" },
    { code: "pt", name: "Portuguese" },
    { code: "ru", name: "Russian" },
    { code: "ja", name: "Japanese" },
    { code: "ko", name: "Korean" },
    { code: "zh", name: "Chinese" },
  ];

  return (
    <Box>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mb={3}
      >
        <Typography variant="h4">Dialogues</Typography>
      </Box>

      <Grid container spacing={3}>
        {images.map((image) => (
          <Grid item xs={12} key={image.id}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" mb={2}>
                  <Typography variant="h6" sx={{ flexGrow: 1 }}>
                    {image.name}
                  </Typography>
                  <Button
                    variant="outlined"
                    startIcon={<Add />}
                    onClick={() => handleAddDialogue(image)}
                  >
                    Add Dialogue
                  </Button>
                </Box>

                {image.dialogues.length === 0 ? (
                  <Typography color="text.secondary">
                    No dialogues yet. Add one to get started.
                  </Typography>
                ) : (
                  <Grid container spacing={2}>
                    {image.dialogues.map((dialogue) => (
                      <Grid item xs={12} sm={6} md={4} key={dialogue.id}>
                        <Card variant="outlined">
                          <CardContent>
                            <Box display="flex" alignItems="center" mb={1}>
                              <Language sx={{ mr: 1, fontSize: 20 }} />
                              <Chip
                                label={
                                  languages.find(
                                    (l) => l.code === dialogue.language
                                  )?.name || dialogue.language
                                }
                                size="small"
                                color="primary"
                              />
                              {dialogue.isDefault && (
                                <Chip
                                  label="Default"
                                  size="small"
                                  color="success"
                                  sx={{ ml: 1 }}
                                />
                              )}
                            </Box>
                            <Typography variant="body2" paragraph>
                              {dialogue.text}
                            </Typography>
                            {dialogue.voiceId && (
                              <Typography
                                variant="caption"
                                color="text.secondary"
                              >
                                Voice: {dialogue.voiceId}
                              </Typography>
                            )}
                          </CardContent>
                          <CardActions>
                            <IconButton
                              size="small"
                              onClick={() =>
                                handleEditDialogue(image, dialogue)
                              }
                            >
                              <Edit />
                            </IconButton>
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => {
                                if (window.confirm("Delete this dialogue?")) {
                                  // Handle delete
                                }
                              }}
                            >
                              <Delete />
                            </IconButton>
                          </CardActions>
                        </Card>
                      </Grid>
                    ))}
                  </Grid>
                )}
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <DialogueDialog
        open={dialogueDialogOpen}
        onClose={() => setDialogueDialogOpen(false)}
        image={selectedImage}
        dialogue={selectedDialogue}
        languages={languages}
      />
    </Box>
  );
};

export default Dialogues;

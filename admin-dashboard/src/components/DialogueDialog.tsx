import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Box,
  Typography,
} from "@mui/material";
import { createDialogue, updateDialogue } from "../store/slices/dialogueSlice";
import { useAppDispatch } from "../store/hooks";

interface DialogueDialogProps {
  open: boolean;
  onClose: () => void;
  image: any;
  dialogue: any;
  languages: Array<{ code: string; name: string }>;
}

export const DialogueDialog: React.FC<DialogueDialogProps> = ({
  open,
  onClose,
  image,
  dialogue,
  languages,
}) => {
  const dispatch = useAppDispatch();
  const [formData, setFormData] = useState({
    text: "",
    language: "en",
    voiceId: "",
    isDefault: false,
  });

  useEffect(() => {
    if (dialogue) {
      setFormData({
        text: dialogue.text,
        language: dialogue.language,
        voiceId: dialogue.voiceId || "",
        isDefault: dialogue.isDefault,
      });
    } else {
      setFormData({
        text: "",
        language: "en",
        voiceId: "",
        isDefault: false,
      });
    }
  }, [dialogue]);

  const handleSubmit = async () => {
    if (!formData.text || !image) return;

    try {
      if (dialogue) {
        // Update existing dialogue
        await dispatch(
          updateDialogue({
            id: dialogue.id,
            data: formData,
          })
        ).unwrap();
      } else {
        // Create new dialogue
        await dispatch(
          createDialogue({
            imageId: image.id,
            ...formData,
          })
        ).unwrap();
      }
      onClose();
    } catch (error) {
      console.error("Failed to save dialogue:", error);
    }
  };

  const handleClose = () => {
    setFormData({
      text: "",
      language: "en",
      voiceId: "",
      isDefault: false,
    });
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>{dialogue ? "Edit Dialogue" : "Add Dialogue"}</DialogTitle>
      <DialogContent>
        <Box sx={{ mb: 2 }}>
          <Typography variant="subtitle2" color="text.secondary">
            Image: {image?.name}
          </Typography>
        </Box>

        <TextField
          autoFocus
          margin="dense"
          label="Dialogue Text"
          fullWidth
          multiline
          rows={4}
          variant="outlined"
          value={formData.text}
          onChange={(e) => setFormData({ ...formData, text: e.target.value })}
          sx={{ mb: 2 }}
        />

        <FormControl fullWidth sx={{ mb: 2 }}>
          <InputLabel>Language</InputLabel>
          <Select
            value={formData.language}
            label="Language"
            onChange={(e) =>
              setFormData({ ...formData, language: e.target.value })
            }
          >
            {languages.map((lang) => (
              <MenuItem key={lang.code} value={lang.code}>
                {lang.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <TextField
          margin="dense"
          label="Voice ID (Optional)"
          fullWidth
          variant="outlined"
          value={formData.voiceId}
          onChange={(e) =>
            setFormData({ ...formData, voiceId: e.target.value })
          }
          sx={{ mb: 2 }}
        />

        <FormControlLabel
          control={
            <Switch
              checked={formData.isDefault}
              onChange={(e) =>
                setFormData({ ...formData, isDefault: e.target.checked })
              }
            />
          }
          label="Set as default dialogue for this language"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          disabled={!formData.text}
        >
          {dialogue ? "Update" : "Add"} Dialogue
        </Button>
      </DialogActions>
    </Dialog>
  );
};

import React, { useState } from "react";
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
  FormControlLabel,
  Switch,
  Box,
  Typography,
  SelectChangeEvent,
} from "@mui/material";
import { EmotionSelector } from "./EmotionSelector";
import type { Dialogue } from "../store/slices/dialogueSlice";

interface DialogueEditorProps {
  open: boolean;
  dialogue?: Dialogue;
  onSave: (
    dialogue:
      | Omit<Dialogue, "id" | "createdAt" | "updatedAt">
      | { id: string; data: Partial<Dialogue> }
  ) => void;
  onClose: () => void;
  imageId: string;
}

export const DialogueEditor: React.FC<DialogueEditorProps> = ({
  open,
  dialogue,
  onSave,
  onClose,
  imageId,
}) => {
  const [text, setText] = useState(dialogue?.text || "");
  const [language, setLanguage] = useState(dialogue?.language || "en");
  const [voiceId, setVoiceId] = useState(dialogue?.voiceId || "");
  const [emotion, setEmotion] = useState(dialogue?.emotion || "neutral");
  const [isDefault, setIsDefault] = useState(dialogue?.isDefault || false);

  const handleSave = () => {
    if (dialogue) {
      // Update existing dialogue
      onSave({
        id: dialogue.id,
        data: {
          text,
          language,
          voiceId: voiceId || undefined,
          emotion: emotion || undefined,
          isDefault,
        },
      });
    } else {
      // Create new dialogue
      onSave({
        imageId,
        text,
        language,
        voiceId: voiceId || undefined,
        emotion: emotion || undefined,
        isDefault,
      });
    }
    onClose();
  };

  const languageOptions = [
    { value: "en", label: "English" },
    { value: "es", label: "Spanish" },
    { value: "fr", label: "French" },
    { value: "de", label: "German" },
    { value: "it", label: "Italian" },
    { value: "pt", label: "Portuguese" },
  ];

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {dialogue ? "Edit Dialogue" : "Create New Dialogue"}
      </DialogTitle>
      <DialogContent>
        <Box sx={{ display: "flex", flexDirection: "column", gap: 2, mt: 1 }}>
          <TextField
            label="Dialogue Text"
            multiline
            rows={3}
            value={text}
            onChange={(e) => setText(e.target.value)}
            fullWidth
            required
          />

          <FormControl fullWidth>
            <InputLabel>Language</InputLabel>
            <Select
              value={language}
              label="Language"
              onChange={(e) => setLanguage(e.target.value as string)}
            >
              {languageOptions.map((lang) => (
                <MenuItem key={lang.value} value={lang.value}>
                  {lang.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <TextField
            label="Voice ID (optional)"
            value={voiceId}
            onChange={(e) => setVoiceId(e.target.value)}
            fullWidth
          />

          <EmotionSelector
            value={emotion}
            onChange={setEmotion}
            label="Emotion"
            fullWidth
          />

          <FormControlLabel
            control={
              <Switch
                checked={isDefault}
                onChange={(e) => setIsDefault(e.target.checked)}
              />
            }
            label="Set as default dialogue"
          />

          <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              Emotion Preview:
            </Typography>
            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <Typography variant="body2">
                {emotion === "happy" &&
                  "😊 Happy - Smiling with raised eyebrows"}
                {emotion === "surprised" &&
                  "😲 Surprised - Wide eyes and open mouth"}
                {emotion === "serious" &&
                  "😐 Serious - Lowered eyebrows and straight mouth"}
                {emotion === "neutral" &&
                  "😐 Neutral - Relaxed facial expression"}
              </Typography>
            </Box>
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          onClick={handleSave}
          variant="contained"
          disabled={!text.trim()}
        >
          {dialogue ? "Update" : "Create"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

import React from "react";
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  SelectChangeEvent,
  Box,
  Typography,
} from "@mui/material";
import { EmojiEmotions } from "@mui/icons-material";

interface EmotionSelectorProps {
  value: string;
  onChange: (value: string) => void;
  label?: string;
  fullWidth?: boolean;
}

const emotionOptions = [
  { value: "neutral", label: "Neutral", emoji: "😐" },
  { value: "happy", label: "Happy", emoji: "😊" },
  { value: "surprised", label: "Surprised", emoji: "😲" },
  { value: "serious", label: "Serious", emoji: "😐" },
];

export const EmotionSelector: React.FC<EmotionSelectorProps> = ({
  value,
  onChange,
  label = "Emotion",
  fullWidth = false,
}) => {
  const handleChange = (event: SelectChangeEvent<string>) => {
    onChange(event.target.value as string);
  };

  return (
    <FormControl fullWidth={fullWidth} size="small">
      <InputLabel>{label}</InputLabel>
      <Select
        value={value}
        label={label}
        onChange={handleChange}
        startAdornment={<EmojiEmotions sx={{ mr: 1 }} />}
      >
        {emotionOptions.map((emotion) => (
          <MenuItem key={emotion.value} value={emotion.value}>
            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <span>{emotion.emoji}</span>
              <Typography variant="body2">{emotion.label}</Typography>
            </Box>
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};

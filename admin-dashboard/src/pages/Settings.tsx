import React, { useState } from "react";
import {
  Typography,
  Paper,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Slider,
  TextField,
  Button,
  Divider,
  Alert,
} from "@mui/material";

export default function Settings() {
  // Audio settings state
  const [ambientAudioEnabled, setAmbientAudioEnabled] = useState(true);
  const [ambientAudioVolume, setAmbientAudioVolume] = useState(30);
  const [ambientAudioType, setAmbientAudioType] = useState("soft_music");
  const [audioFadeDuration, setAudioFadeDuration] = useState(2000);

  // Lighting settings state
  const [dynamicLightingEnabled, setDynamicLightingEnabled] = useState(true);
  const [shadowIntensity, setShadowIntensity] = useState(30);

  const audioTypes = [
    { value: "soft_music", label: "Soft Background Music" },
    { value: "nature_sounds", label: "Nature Sounds" },
    { value: "urban_ambience", label: "Urban Ambience" },
    { value: "none", label: "None" },
  ];

  const handleSave = () => {
    // In a real implementation, this would save to the backend
    console.log("Settings saved:", {
      ambientAudioEnabled,
      ambientAudioVolume,
      ambientAudioType,
      audioFadeDuration,
      dynamicLightingEnabled,
      shadowIntensity,
    });

    // Show success message
    alert("Settings saved successfully!");
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>

      <Alert severity="info" sx={{ mb: 3 }}>
        Configure environmental realism settings for your AR experience
      </Alert>

      <Box sx={{ display: "flex", flexDirection: "column", gap: 4 }}>
        {/* Audio Settings Section */}
        <Box>
          <Typography variant="h6" gutterBottom>
            Audio Settings
          </Typography>

          <FormControlLabel
            control={
              <Switch
                checked={ambientAudioEnabled}
                onChange={(e) => setAmbientAudioEnabled(e.target.checked)}
              />
            }
            label="Enable Ambient Background Audio"
          />

          {ambientAudioEnabled && (
            <Box
              sx={{
                mt: 2,
                pl: 4,
                display: "flex",
                flexDirection: "column",
                gap: 3,
              }}
            >
              <FormControl fullWidth>
                <InputLabel>Ambient Audio Type</InputLabel>
                <Select
                  value={ambientAudioType}
                  label="Ambient Audio Type"
                  onChange={(e) =>
                    setAmbientAudioType(e.target.value as string)
                  }
                >
                  {audioTypes.map((type) => (
                    <MenuItem key={type.value} value={type.value}>
                      {type.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <Box>
                <Typography gutterBottom>
                  Ambient Volume: {ambientAudioVolume}%
                </Typography>
                <Slider
                  value={ambientAudioVolume}
                  onChange={(_, value) =>
                    setAmbientAudioVolume(value as number)
                  }
                  min={0}
                  max={100}
                  step={5}
                  valueLabelDisplay="auto"
                />
              </Box>

              <Box>
                <Typography gutterBottom>
                  Audio Fade Duration: {audioFadeDuration}ms
                </Typography>
                <Slider
                  value={audioFadeDuration}
                  onChange={(_, value) => setAudioFadeDuration(value as number)}
                  min={500}
                  max={5000}
                  step={100}
                  valueLabelDisplay="auto"
                />
              </Box>
            </Box>
          )}
        </Box>

        <Divider />

        {/* Visual Settings Section */}
        <Box>
          <Typography variant="h6" gutterBottom>
            Visual Settings
          </Typography>

          <FormControlLabel
            control={
              <Switch
                checked={dynamicLightingEnabled}
                onChange={(e) => setDynamicLightingEnabled(e.target.checked)}
              />
            }
            label="Enable Dynamic Lighting"
          />

          <Box
            sx={{
              mt: 2,
              pl: 4,
              display: "flex",
              flexDirection: "column",
              gap: 3,
            }}
          >
            <Box>
              <Typography gutterBottom>
                Shadow Intensity: {shadowIntensity}%
              </Typography>
              <Slider
                value={shadowIntensity}
                onChange={(_, value) => setShadowIntensity(value as number)}
                min={0}
                max={100}
                step={5}
                valueLabelDisplay="auto"
              />
            </Box>
          </Box>
        </Box>

        <Divider />

        {/* Save Button */}
        <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
          <Button variant="contained" onClick={handleSave}>
            Save Settings
          </Button>
        </Box>
      </Box>
    </Paper>
  );
}

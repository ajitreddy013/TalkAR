import React, { useState, useEffect } from "react";
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
  Snackbar,
  LinearProgress,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Grid,
} from "@mui/material";
import { ExpandMore as ExpandMoreIcon } from "@mui/icons-material";
import { SettingsService, SettingsData } from "../services/settingsService";

export default function Settings() {
  // Settings state with default values
  const [settings, setSettings] = useState<SettingsData>({
    ambientAudioEnabled: true,
    ambientAudioVolume: 30,
    ambientAudioType: "soft_music",
    audioFadeDuration: 2000,
    dynamicLightingEnabled: true,
    shadowIntensity: 30,
    avatarScale: 1.0,
    avatarPositionX: 0,
    avatarPositionY: 0,
    avatarPositionZ: 0,
    maxFrameRate: 30,
    qualitySetting: "medium",
    showDebugInfo: false,
    enableGestures: true,
  });

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const audioTypes = [
    { value: "soft_music", label: "Soft Background Music" },
    { value: "nature_sounds", label: "Nature Sounds" },
    { value: "urban_ambience", label: "Urban Ambience" },
    { value: "none", label: "None" },
  ];

  const qualityOptions = [
    { value: "low", label: "Low Quality (Best Performance)" },
    { value: "medium", label: "Medium Quality" },
    { value: "high", label: "High Quality" },
    { value: "ultra", label: "Ultra Quality (Best Visuals)" },
  ];

  // Load settings on component mount
  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await SettingsService.getSettings();
      if (response.data.success) {
        setSettings(response.data.settings);
      }
    } catch (err) {
      console.error("Failed to load settings:", err);
      setError("Failed to load settings");
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const response = await SettingsService.updateSettings(settings);
      if (response.data.success) {
        setSuccess(true);
      } else {
        setError(response.data.message || "Failed to save settings");
      }
    } catch (err) {
      console.error("Failed to save settings:", err);
      setError("Failed to save settings");
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    setSaving(true);
    setError(null);
    try {
      const response = await SettingsService.resetSettings();
      if (response.data.success) {
        // Reload settings after reset
        await loadSettings();
        setSuccess(true);
      } else {
        setError(response.data.message || "Failed to reset settings");
      }
    } catch (err) {
      console.error("Failed to reset settings:", err);
      setError("Failed to reset settings");
    } finally {
      setSaving(false);
    }
  };

  const handleChange = (field: keyof SettingsData, value: any) => {
    setSettings((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  if (loading) {
    return <LinearProgress />;
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Alert severity="info" sx={{ mb: 3 }}>
        Configure environmental realism settings for your AR experience
      </Alert>

      <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        {/* Audio Settings Section */}
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">Audio Settings</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.ambientAudioEnabled}
                    onChange={(e) =>
                      handleChange("ambientAudioEnabled", e.target.checked)
                    }
                  />
                }
                label="Enable Ambient Background Audio"
              />

              {settings.ambientAudioEnabled && (
                <Box
                  sx={{
                    pl: 4,
                    display: "flex",
                    flexDirection: "column",
                    gap: 3,
                  }}
                >
                  <FormControl fullWidth>
                    <InputLabel>Ambient Audio Type</InputLabel>
                    <Select
                      value={settings.ambientAudioType}
                      label="Ambient Audio Type"
                      onChange={(e) =>
                        handleChange(
                          "ambientAudioType",
                          e.target.value as string
                        )
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
                      Ambient Volume: {settings.ambientAudioVolume}%
                    </Typography>
                    <Slider
                      value={settings.ambientAudioVolume}
                      onChange={(_, value) =>
                        handleChange("ambientAudioVolume", value as number)
                      }
                      min={0}
                      max={100}
                      step={5}
                      valueLabelDisplay="auto"
                    />
                  </Box>

                  <Box>
                    <Typography gutterBottom>
                      Audio Fade Duration: {settings.audioFadeDuration}ms
                    </Typography>
                    <Slider
                      value={settings.audioFadeDuration}
                      onChange={(_, value) =>
                        handleChange("audioFadeDuration", value as number)
                      }
                      min={500}
                      max={5000}
                      step={100}
                      valueLabelDisplay="auto"
                    />
                  </Box>
                </Box>
              )}
            </Box>
          </AccordionDetails>
        </Accordion>

        {/* Visual Settings Section */}
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">Visual Settings</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.dynamicLightingEnabled}
                    onChange={(e) =>
                      handleChange("dynamicLightingEnabled", e.target.checked)
                    }
                  />
                }
                label="Enable Dynamic Lighting"
              />

              <Box
                sx={{
                  pl: 4,
                  display: "flex",
                  flexDirection: "column",
                  gap: 3,
                }}
              >
                <Box>
                  <Typography gutterBottom>
                    Shadow Intensity: {settings.shadowIntensity}%
                  </Typography>
                  <Slider
                    value={settings.shadowIntensity}
                    onChange={(_, value) =>
                      handleChange("shadowIntensity", value as number)
                    }
                    min={0}
                    max={100}
                    step={5}
                    valueLabelDisplay="auto"
                  />
                </Box>
              </Box>

              <Divider />

              <Typography variant="subtitle1">
                Avatar Position & Scale
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} md={4}>
                  <Box>
                    <Typography gutterBottom>
                      Scale: {settings.avatarScale.toFixed(2)}
                    </Typography>
                    <Slider
                      value={settings.avatarScale}
                      onChange={(_, value) =>
                        handleChange("avatarScale", value as number)
                      }
                      min={0.5}
                      max={2.0}
                      step={0.05}
                      valueLabelDisplay="auto"
                    />
                  </Box>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Box>
                    <Typography gutterBottom>
                      X Position: {settings.avatarPositionX.toFixed(2)}
                    </Typography>
                    <Slider
                      value={settings.avatarPositionX}
                      onChange={(_, value) =>
                        handleChange("avatarPositionX", value as number)
                      }
                      min={-1.0}
                      max={1.0}
                      step={0.05}
                      valueLabelDisplay="auto"
                    />
                  </Box>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Box>
                    <Typography gutterBottom>
                      Y Position: {settings.avatarPositionY.toFixed(2)}
                    </Typography>
                    <Slider
                      value={settings.avatarPositionY}
                      onChange={(_, value) =>
                        handleChange("avatarPositionY", value as number)
                      }
                      min={-1.0}
                      max={1.0}
                      step={0.05}
                      valueLabelDisplay="auto"
                    />
                  </Box>
                </Grid>
                <Grid item xs={12}>
                  <Box>
                    <Typography gutterBottom>
                      Z Position: {settings.avatarPositionZ.toFixed(2)}
                    </Typography>
                    <Slider
                      value={settings.avatarPositionZ}
                      onChange={(_, value) =>
                        handleChange("avatarPositionZ", value as number)
                      }
                      min={-1.0}
                      max={1.0}
                      step={0.05}
                      valueLabelDisplay="auto"
                    />
                  </Box>
                </Grid>
              </Grid>
            </Box>
          </AccordionDetails>
        </Accordion>

        {/* Performance Settings Section */}
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">Performance Settings</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
              <Box>
                <Typography gutterBottom>
                  Max Frame Rate: {settings.maxFrameRate} FPS
                </Typography>
                <Slider
                  value={settings.maxFrameRate}
                  onChange={(_, value) =>
                    handleChange("maxFrameRate", value as number)
                  }
                  min={15}
                  max={60}
                  step={5}
                  valueLabelDisplay="auto"
                  marks={[
                    { value: 15, label: "15" },
                    { value: 30, label: "30" },
                    { value: 60, label: "60" },
                  ]}
                />
              </Box>

              <FormControl fullWidth>
                <InputLabel>Quality Setting</InputLabel>
                <Select
                  value={settings.qualitySetting}
                  label="Quality Setting"
                  onChange={(e) =>
                    handleChange(
                      "qualitySetting",
                      e.target.value as "low" | "medium" | "high" | "ultra"
                    )
                  }
                >
                  {qualityOptions.map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
          </AccordionDetails>
        </Accordion>

        {/* UI Settings Section */}
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">UI Settings</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.showDebugInfo}
                    onChange={(e) =>
                      handleChange("showDebugInfo", e.target.checked)
                    }
                  />
                }
                label="Show Debug Information"
              />

              <FormControlLabel
                control={
                  <Switch
                    checked={settings.enableGestures}
                    onChange={(e) =>
                      handleChange("enableGestures", e.target.checked)
                    }
                  />
                }
                label="Enable Gesture Controls"
              />
            </Box>
          </AccordionDetails>
        </Accordion>

        {/* Save Button */}
        <Box
          sx={{ display: "flex", justifyContent: "flex-end", gap: 2, mt: 2 }}
        >
          <Button variant="outlined" onClick={handleReset} disabled={saving}>
            Reset to Defaults
          </Button>
          <Button variant="contained" onClick={handleSave} disabled={saving}>
            {saving ? "Saving..." : "Save Settings"}
          </Button>
        </Box>
      </Box>

      <Snackbar
        open={success}
        autoHideDuration={3000}
        onClose={() => setSuccess(false)}
        message="Settings saved successfully!"
      />
    </Paper>
  );
}

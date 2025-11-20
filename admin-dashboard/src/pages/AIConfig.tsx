import React, { useState, useEffect } from "react";
import {
  Typography,
  Paper,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Divider,
  Alert,
  CircularProgress,
} from "@mui/material";
import { api } from "../services/api";

export default function AIConfig() {
  // AI Configuration state
  const [defaultTone, setDefaultTone] = useState("friendly");
  const [defaultLanguage, setDefaultLanguage] = useState("en");
  const [defaultVoiceId, setDefaultVoiceId] = useState("");

  // UI state
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Tone options
  const toneOptions = [
    { value: "friendly", label: "Friendly" },
    { value: "professional", label: "Professional" },
    { value: "casual", label: "Casual" },
    { value: "enthusiastic", label: "Enthusiastic" },
    { value: "informative", label: "Informative" },
  ];

  // Language options
  const languageOptions = [
    { value: "en", label: "English" },
    { value: "es", label: "Spanish" },
    { value: "fr", label: "French" },
    { value: "de", label: "German" },
    { value: "it", label: "Italian" },
    { value: "pt", label: "Portuguese" },
  ];

  // Load initial configuration
  useEffect(() => {
    loadConfiguration();
  }, []);

  const loadConfiguration = async () => {
    try {
      setLoading(true);
      const response = await api.get("/admin/config");
      const config = response.data.config;

      if (config) {
        setDefaultTone(config.defaultTone || "friendly");
        setDefaultLanguage(config.defaultLanguage || "en");
        setDefaultVoiceId(config.voiceId || "");
      }

      setLoading(false);
    } catch (err) {
      console.error("Error loading configuration:", err);
      setError("Failed to load AI configuration");
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      setError(null);
      setSuccess(null);

      await api.post("/admin/update-config", {
        defaultTone,
        defaultLanguage,
        voiceId: defaultVoiceId,
      });

      setSuccess("AI configuration saved successfully!");
      setSaving(false);
    } catch (err) {
      console.error("Error saving configuration:", err);
      setError("Failed to save AI configuration");
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="200px"
      >
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        AI Configuration
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {success}
        </Alert>
      )}

      <Alert severity="info" sx={{ mb: 3 }}>
        Configure default settings for AI-generated content. Changes apply to new generations immediately.
      </Alert>

      <Box sx={{ display: "flex", flexDirection: "column", gap: 4 }}>
        {/* Default Tone Setting */}
        <Box>
          <Typography variant="h6" gutterBottom>
            Default Tone
          </Typography>

          <FormControl fullWidth>
            <InputLabel>Default Tone</InputLabel>
            <Select
              value={defaultTone}
              label="Default Tone"
              onChange={(e) => setDefaultTone(e.target.value as string)}
            >
              {toneOptions.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

        <Divider />

        {/* Default Language Setting */}
        <Box>
          <Typography variant="h6" gutterBottom>
            Default Language
          </Typography>

          <FormControl fullWidth>
            <InputLabel>Default Language</InputLabel>
            <Select
              value={defaultLanguage}
              label="Default Language"
              onChange={(e) => setDefaultLanguage(e.target.value as string)}
            >
              {languageOptions.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

        <Divider />

        {/* Save Button */}
        <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
          <Button variant="contained" onClick={handleSave} disabled={saving}>
            {saving ? "Saving..." : "Save Configuration"}
          </Button>
        </Box>
      </Box>
    </Paper>
  );
}

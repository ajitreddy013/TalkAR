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
  Button,
  Divider,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
} from "@mui/material";
import { api } from "../services/api";

export default function AIConfig() {
  // AI Configuration state
  const [defaultTone, setDefaultTone] = useState("friendly");
  const [defaultLanguage, setDefaultLanguage] = useState("en");
  const [defaultAvatar, setDefaultAvatar] = useState("");
  const [availableAvatars, setAvailableAvatars] = useState<any[]>([]);

  // AI Logs state
  const [aiLogs, setAiLogs] = useState<any[]>([]);
  const [loadingLogs, setLoadingLogs] = useState(true);

  // UI state
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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

  // Load initial configuration and avatars
  useEffect(() => {
    loadConfiguration();
    loadAvatars();
    loadAILogs();
  }, []);

  const loadConfiguration = async () => {
    try {
      setLoading(true);
      const response = await api.get("/ai-config");
      const configs = response.data.configs;

      setDefaultTone(configs.default_tone || "friendly");
      setDefaultLanguage(configs.default_language || "en");
      setDefaultAvatar(configs.default_avatar_id || "");

      setLoading(false);
    } catch (err) {
      console.error("Error loading configuration:", err);
      setError("Failed to load AI configuration");
      setLoading(false);
    }
  };

  const loadAvatars = async () => {
    try {
      const response = await api.get("/avatars");
      setAvailableAvatars(response.data);
    } catch (err) {
      console.error("Error loading avatars:", err);
      setError("Failed to load avatars");
    }
  };

  const loadAILogs = async () => {
    try {
      setLoadingLogs(true);
      const response = await api.get("/analytics/ai-pipeline-events");
      setAiLogs(response.data.aiPipelineEvents.recent || []);
      setLoadingLogs(false);
    } catch (err) {
      console.error("Error loading AI logs:", err);
      setError("Failed to load AI logs");
      setLoadingLogs(false);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);

      // Save default tone
      await api.post("/ai-config/defaults/tone", { tone: defaultTone });

      // Save default language
      await api.post("/ai-config/defaults/language", {
        language: defaultLanguage,
      });

      // Save default avatar
      if (defaultAvatar) {
        await api.post("/ai-config/defaults/avatar", {
          avatarId: defaultAvatar,
        });
      }

      setSaving(false);
      alert("AI configuration saved successfully!");
    } catch (err) {
      console.error("Error saving configuration:", err);
      setError("Failed to save AI configuration");
      setSaving(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "completed":
        return "success";
      case "failed":
        return "error";
      case "started":
        return "primary";
      default:
        return "default";
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

      <Alert severity="info" sx={{ mb: 3 }}>
        Configure default settings for AI-generated content and view recent AI
        pipeline events
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

        {/* Default Avatar Setting */}
        <Box>
          <Typography variant="h6" gutterBottom>
            Default Avatar
          </Typography>

          <FormControl fullWidth>
            <InputLabel>Default Avatar</InputLabel>
            <Select
              value={defaultAvatar}
              label="Default Avatar"
              onChange={(e) => setDefaultAvatar(e.target.value as string)}
              displayEmpty
            >
              <MenuItem value="">
                <em>None</em>
              </MenuItem>
              {availableAvatars.map((avatar) => (
                <MenuItem key={avatar.id} value={avatar.id}>
                  {avatar.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

        <Divider />

        {/* Recent AI Logs */}
        <Box>
          <Typography variant="h6" gutterBottom>
            Recent AI Pipeline Events
          </Typography>

          {loadingLogs ? (
            <Box display="flex" justifyContent="center" my={2}>
              <CircularProgress size={24} />
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Event Type</TableCell>
                    <TableCell>Product</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Details</TableCell>
                    <TableCell>Timestamp</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {aiLogs.length > 0 ? (
                    aiLogs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell>
                          <Chip
                            label={log.eventType.replace("_", " ")}
                            size="small"
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>{log.productName || "N/A"}</TableCell>
                        <TableCell>
                          <Chip
                            label={log.status}
                            color={getStatusColor(log.status)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{log.details}</TableCell>
                        <TableCell>
                          {new Date(log.timestamp).toLocaleTimeString()}
                        </TableCell>
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        No recent AI pipeline events
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
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

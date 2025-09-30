import React, { useState } from "react";
import {
  Box,
  Typography,
  Card,
  CardContent,
  TextField,
  Button,
  Switch,
  FormControlLabel,
  Divider,
  Alert,
} from "@mui/material";
import { Save } from "@mui/icons-material";

const Settings: React.FC = () => {
  const [settings, setSettings] = useState({
    apiUrl: process.env.REACT_APP_API_URL || "http://localhost:3000/api/v1",
    syncApiUrl: "https://api.sync.com/v1",
    maxFileSize: "10",
    allowedFileTypes: "image/jpeg,image/png,image/gif,image/webp",
    enableNotifications: true,
    autoSave: true,
  });

  const [saved, setSaved] = useState(false);

  const handleChange = (field: string, value: any) => {
    setSettings((prev) => ({ ...prev, [field]: value }));
  };

  const handleSave = () => {
    // In a real app, this would save to backend
    console.log("Saving settings:", settings);
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>

      {saved && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Settings saved successfully!
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                API Configuration
              </Typography>
              <TextField
                fullWidth
                label="API Base URL"
                value={settings.apiUrl}
                onChange={(e) => handleChange("apiUrl", e.target.value)}
                margin="normal"
              />
              <TextField
                fullWidth
                label="Sync API URL"
                value={settings.syncApiUrl}
                onChange={(e) => handleChange("syncApiUrl", e.target.value)}
                margin="normal"
              />
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                File Upload Settings
              </Typography>
              <TextField
                fullWidth
                label="Max File Size (MB)"
                type="number"
                value={settings.maxFileSize}
                onChange={(e) => handleChange("maxFileSize", e.target.value)}
                margin="normal"
              />
              <TextField
                fullWidth
                label="Allowed File Types"
                value={settings.allowedFileTypes}
                onChange={(e) =>
                  handleChange("allowedFileTypes", e.target.value)
                }
                margin="normal"
              />
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Application Settings
              </Typography>
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.enableNotifications}
                    onChange={(e) =>
                      handleChange("enableNotifications", e.target.checked)
                    }
                  />
                }
                label="Enable Notifications"
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.autoSave}
                    onChange={(e) => handleChange("autoSave", e.target.checked)}
                  />
                }
                label="Auto Save Changes"
              />
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Box display="flex" justifyContent="flex-end">
            <Button
              variant="contained"
              startIcon={<Save />}
              onClick={handleSave}
              size="large"
            >
              Save Settings
            </Button>
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Settings;

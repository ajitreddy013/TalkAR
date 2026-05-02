import React, { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import { AuthService } from "../services/authService";

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("admin@talkar.com");
  const [password, setPassword] = useState("admin123");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response = await AuthService.login({ email, password });
      localStorage.setItem("token", response.data.token);
      navigate("/dashboard", { replace: true });
    } catch {
      setError("Login failed. Check admin credentials.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ minHeight: "100vh", display: "grid", placeItems: "center", p: 2 }}>
      <Paper sx={{ width: "100%", maxWidth: 420, p: 3 }}>
        <Typography variant="h5" sx={{ mb: 2 }}>
          TalkAR Admin Login
        </Typography>
        {error ? <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert> : null}
        <Stack component="form" spacing={2} onSubmit={onSubmit}>
          <TextField
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            fullWidth
          />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            fullWidth
          />
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? "Signing in..." : "Sign in"}
          </Button>
        </Stack>
      </Paper>
    </Box>
  );
}

import React from "react";
import { Routes, Route } from "react-router-dom";
import { Box, Container, Typography, Paper, Button } from "@mui/material";
import Layout from "./components/Layout";
import Images from "./pages/Images";
import Dialogues from "./pages/Dialogues";
import Analytics from "./pages/Analytics";
import Settings from "./pages/Settings";

function App() {
  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      <Layout>
        <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/images" element={<Images />} />
            <Route path="/dialogues" element={<Dialogues />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/settings" element={<Settings />} />
          </Routes>
        </Container>
      </Layout>
    </Box>
  );
}

function Dashboard() {
  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        TalkAR Admin Dashboard
      </Typography>
      <Typography variant="body1" paragraph>
        Welcome to the TalkAR Admin Dashboard. Here you can manage images and scripts for your AR application.
      </Typography>
      <Box sx={{ mt: 3 }}>
        <Button 
          variant="contained" 
          href="/images"
          sx={{ mr: 2 }}
        >
          Manage Images
        </Button>
        <Button 
          variant="outlined" 
          href="/dialogues"
        >
          Manage Scripts
        </Button>
      </Box>
    </Paper>
  );
}

export default App;
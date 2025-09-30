import React from "react";
import { Routes, Route } from "react-router-dom";
import { Box } from "@mui/material";
import Layout from "./components/Layout";
import Dashboard from "./pages/Dashboard";
import Images from "./pages/Images";
import Dialogues from "./pages/Dialogues";
import Analytics from "./pages/Analytics";
import Settings from "./pages/Settings";

function App() {
  return (
    <Box sx={{ display: "flex" }}>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/images" element={<Images />} />
          <Route path="/dialogues" element={<Dialogues />} />
          <Route path="/analytics" element={<Analytics />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </Layout>
    </Box>
  );
}

export default App;

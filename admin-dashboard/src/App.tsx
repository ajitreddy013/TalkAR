import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import AdminShell from "./components/admin/AdminShell";
import LoginPage from "./pages/Login";
import DashboardPage from "./pages/Dashboard";
import ImagesPage from "./pages/Images";
import InteractionsPage from "./pages/Interactions";
import LiveMonitorPage from "./pages/LiveMonitor";
import AlertsPage from "./pages/Alerts";
import AnalyticsPage from "./pages/Analytics";
import AIConfigPage from "./pages/AIConfig";
import SettingsPage from "./pages/Settings";

function App() {
  const hasToken = !!localStorage.getItem("token");

  return (
    <Routes>
      <Route
        path="/login"
        element={hasToken ? <Navigate to="/dashboard" replace /> : <LoginPage />}
      />
      <Route path="/" element={<AdminShell />}>
        <Route
          index
          element={<Navigate to={hasToken ? "/dashboard" : "/login"} replace />}
        />
        <Route
          path="dashboard"
          element={hasToken ? <DashboardPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="images"
          element={hasToken ? <ImagesPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="interactions"
          element={hasToken ? <InteractionsPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="live-monitor"
          element={hasToken ? <LiveMonitorPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="alerts"
          element={hasToken ? <AlertsPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="analytics"
          element={hasToken ? <AnalyticsPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="ai-config"
          element={hasToken ? <AIConfigPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="settings"
          element={hasToken ? <SettingsPage /> : <Navigate to="/login" replace />}
        />
      </Route>
      <Route
        path="*"
        element={<Navigate to={hasToken ? "/dashboard" : "/login"} replace />}
      />
    </Routes>
  );
}

export default App;

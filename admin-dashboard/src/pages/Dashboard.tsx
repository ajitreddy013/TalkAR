import React, { useEffect, useState } from "react";
import {
  Grid,
  Paper,
  Typography,
  Box,
  LinearProgress,
  Alert,
} from "@mui/material";
import { QrCodeScanner, PlayArrow, ThumbUp, AccessTime } from "@mui/icons-material";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import { AnalyticsService, AggregatedMetric } from "../services/analyticsService";
import PageHeader from "../components/admin/PageHeader";
import StatCard from "../components/admin/StatCard";
import { ImageService, PosterOpsMetrics } from "../services/imageService";

const Dashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<AggregatedMetric[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [posterOps, setPosterOps] = useState<PosterOpsMetrics | null>(null);

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const fetchAnalytics = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await AnalyticsService.getAggregatedMetrics();
      const sortedMetrics = response.data.sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
      setMetrics(sortedMetrics);
      const posterOpsResponse = await ImageService.getPosterOpsMetrics();
      setPosterOps(posterOpsResponse.data);
    } catch (err) {
      console.error("Failed to fetch analytics:", err);
      setError("Failed to load analytics data");
    } finally {
      setLoading(false);
    }
  };

  const totalScans = metrics.reduce((sum, m) => sum + m.scans, 0);
  const totalPlays = metrics.reduce((sum, m) => sum + m.plays, 0);
  const avgLatency = metrics.length > 0
    ? Math.round(metrics.reduce((sum, m) => sum + m.avg_latency_ms, 0) / metrics.length)
    : 0;
  const totalLikes = metrics.reduce((sum, m) => sum + m.likes, 0);
  const totalDislikes = metrics.reduce((sum, m) => sum + m.dislikes, 0);
  const likeRate = (totalLikes + totalDislikes) > 0
    ? Math.round((totalLikes / (totalLikes + totalDislikes)) * 100)
    : 0;

  const feedbackData = [
    { name: "Likes", value: totalLikes },
    { name: "Dislikes", value: totalDislikes },
  ];

  if (loading) return <LinearProgress />;

  return (
    <Box>
      <PageHeader title="Dashboard" subtitle="Operational overview of scans, plays, latency, and feedback." />
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
        <Grid item xs={12} sm={6} md={3}><StatCard title="Total Scans" value={totalScans} icon={<QrCodeScanner color="primary" />} /></Grid>
        <Grid item xs={12} sm={6} md={3}><StatCard title="Total Plays" value={totalPlays} icon={<PlayArrow color="success" />} /></Grid>
        <Grid item xs={12} sm={6} md={3}><StatCard title="Avg Latency" value={`${avgLatency}ms`} icon={<AccessTime color="warning" />} /></Grid>
        <Grid item xs={12} sm={6} md={3}><StatCard title="Like Rate" value={`${likeRate}%`} icon={<ThumbUp color="secondary" />} /></Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Artifact Success"
            value={posterOps ? `${Math.round(posterOps.generation.successRate * 100)}%` : "-"}
            icon={<PlayArrow color="success" />}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Median Gen Time"
            value={posterOps?.generation.medianGenerationTimeMs != null ? `${posterOps.generation.medianGenerationTimeMs}ms` : "-"}
            icon={<AccessTime color="warning" />}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Queue Backlog"
            value={posterOps ? posterOps.queueBacklog : "-"}
            icon={<QrCodeScanner color="primary" />}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Preprocess Eligible"
            value={posterOps ? `${posterOps.preprocess.eligible}/${posterOps.preprocess.total}` : "-"}
            icon={<ThumbUp color="secondary" />}
          />
        </Grid>
      </Grid>

      <Grid container spacing={2.5}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2.5, height: 420 }}>
            <Typography variant="h6" gutterBottom>Scans and Plays by Date</Typography>
            <ResponsiveContainer width="100%" height="92%">
              <BarChart data={metrics} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="scans" fill="#1f4db8" name="Scans" radius={[4, 4, 0, 0]} />
                <Bar dataKey="plays" fill="#0f766e" name="Plays" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2.5, height: 420 }}>
            <Typography variant="h6" gutterBottom>Feedback Split</Typography>
            <ResponsiveContainer width="100%" height="92%">
              <PieChart>
                <Pie
                  data={feedbackData}
                  cx="50%"
                  cy="50%"
                  dataKey="value"
                  outerRadius={110}
                  label={({ name, percent }) => `${name}: ${((percent as number) * 100).toFixed(0)}%`}
                >
                  <Cell fill="#2e7d32" />
                  <Cell fill="#d32f2f" />
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;

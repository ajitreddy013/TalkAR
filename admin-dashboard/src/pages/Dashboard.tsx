import React, { useEffect, useState } from "react";
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  LinearProgress,
  Alert,
} from "@mui/material";
import {
  QrCodeScanner,
  PlayArrow,
  ThumbUp,
  AccessTime,
} from "@mui/icons-material";
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

interface StatItem {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
}

const Dashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<AggregatedMetric[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const fetchAnalytics = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await AnalyticsService.getAggregatedMetrics();
      // Sort by date ascending
      const sortedMetrics = response.data.sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
      setMetrics(sortedMetrics);
    } catch (err) {
      console.error("Failed to fetch analytics:", err);
      setError("Failed to load analytics data");
    } finally {
      setLoading(false);
    }
  };

  // Calculate aggregates
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

  const stats: StatItem[] = [
    {
      title: "Total Scans",
      value: totalScans,
      icon: <QrCodeScanner />,
      color: "#1976d2",
    },
    {
      title: "Total Plays",
      value: totalPlays,
      icon: <PlayArrow />,
      color: "#2e7d32",
    },
    {
      title: "Avg Latency",
      value: `${avgLatency}ms`,
      icon: <AccessTime />,
      color: "#ed6c02",
    },
    {
      title: "Like Rate",
      value: `${likeRate}%`,
      icon: <ThumbUp />,
      color: "#9c27b0",
    },
  ];

  const feedbackData = [
    { name: "Likes", value: totalLikes },
    { name: "Dislikes", value: totalDislikes },
  ];

  const COLORS = ["#4caf50", "#f44336"];

  if (loading) {
    return <LinearProgress />;
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {stats.map((stat, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" mb={2}>
                  <Box
                    sx={{
                      backgroundColor: stat.color,
                      color: "white",
                      borderRadius: "50%",
                      p: 1,
                      mr: 2,
                    }}
                  >
                    {stat.icon}
                  </Box>
                  <Typography variant="h6" component="div">
                    {stat.title}
                  </Typography>
                </Box>
                <Typography variant="h4" color="primary">
                  {stat.value}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}

        {/* Charts Section */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3, height: 400 }}>
            <Typography variant="h6" gutterBottom>
              Scans & Plays per Day
            </Typography>
            <ResponsiveContainer width="100%" height="90%">
              <BarChart
                data={metrics}
                margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="scans" fill="#1976d2" name="Scans" />
                <Bar dataKey="plays" fill="#2e7d32" name="Plays" />
              </BarChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3, height: 400 }}>
            <Typography variant="h6" gutterBottom>
              Feedback Overview
            </Typography>
            <ResponsiveContainer width="100%" height="90%">
              <PieChart>
                <Pie
                  data={feedbackData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) =>
                    `${name}: ${((percent as number) * 100).toFixed(0)}%`
                  }
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {feedbackData.map((entry, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={COLORS[index % COLORS.length]}
                    />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;

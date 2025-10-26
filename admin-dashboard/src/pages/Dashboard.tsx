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
  Image as ImageIcon,
  Chat,
  Visibility,
  TrendingUp,
  PlayArrow,
  Error as ErrorIcon,
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
import { fetchImages } from "../store/slices/imageSlice";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { AnalyticsService, AnalyticsData } from "../services/analyticsService";

interface StatItem {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
}

const Dashboard: React.FC = () => {
  const dispatch = useAppDispatch();
  const { images, loading } = useAppSelector((state) => state.images);
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    dispatch(fetchImages());
    fetchAnalytics();
  }, [dispatch]);

  const fetchAnalytics = async () => {
    setAnalyticsLoading(true);
    setError(null);
    try {
      const response = await AnalyticsService.getAnalytics();
      setAnalytics(response.data.analytics);
    } catch (err) {
      console.error("Failed to fetch analytics:", err);
      setError("Failed to load analytics data");
    } finally {
      setAnalyticsLoading(false);
    }
  };

  // Prepare data for charts
  const getImageTriggerData = () => {
    if (!analytics?.imageTriggers?.byImage) return [];
    return analytics.imageTriggers.byImage.map((item) => ({
      name:
        item.imageName.length > 15
          ? `${item.imageName.substring(0, 15)}...`
          : item.imageName,
      triggers: item.count,
    }));
  };

  const getAIPipelineData = () => {
    if (!analytics?.aiPipelineEvents?.byType) return [];
    return analytics.aiPipelineEvents.byType.map((item) => ({
      name: item.eventType.replace("_", " "),
      count: item.count,
    }));
  };

  const getPerformanceData = () => {
    if (!analytics?.performance) return [];
    return [
      { name: "Successful", value: analytics.performance.successfulRequests },
      { name: "Failed", value: analytics.performance.failedRequests },
    ];
  };

  const COLORS = ["#0088FE", "#FF0000"];

  const stats: StatItem[] = [
    {
      title: "Total Images",
      value: images.length,
      icon: <ImageIcon />,
      color: "#1976d2",
    },
    {
      title: "Active Images",
      value: images.filter((img) => img.isActive).length,
      icon: <Visibility />,
      color: "#2e7d32",
    },
    {
      title: "Total Dialogues",
      value: images.reduce((acc, img) => acc + img.dialogues.length, 0),
      icon: <Chat />,
      color: "#ed6c02",
    },
    {
      title: "Languages",
      value: new Set(
        images.flatMap((img) => img.dialogues.map((d) => d.language))
      ).size,
      icon: <TrendingUp />,
      color: "#9c27b0",
    },
  ];

  // Add analytics-based stats
  if (analytics) {
    stats.push({
      title: "Total Triggers",
      value: analytics.imageTriggers.total,
      icon: <PlayArrow />,
      color: "#4caf50",
    });

    stats.push({
      title: "AI Pipeline Errors",
      value: analytics.aiPipelineEvents.errors,
      icon: <ErrorIcon />,
      color: "#f44336",
    });

    stats.push({
      title: "Avg Response Time",
      value: `${analytics.performance.averageResponseTime}ms`,
      icon: <AccessTime />,
      color: "#ff9800",
    });
  }

  if (loading || analyticsLoading) {
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
        {analytics && (
          <>
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3, height: 400 }}>
                <Typography variant="h6" gutterBottom>
                  Image Triggers by Image
                </Typography>
                <ResponsiveContainer width="100%" height="90%">
                  <BarChart
                    data={getImageTriggerData()}
                    margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis
                      dataKey="name"
                      angle={-45}
                      textAnchor="end"
                      height={60}
                    />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="triggers" fill="#8884d8" name="Triggers" />
                  </BarChart>
                </ResponsiveContainer>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3, height: 400 }}>
                <Typography variant="h6" gutterBottom>
                  AI Pipeline Events
                </Typography>
                <ResponsiveContainer width="100%" height="90%">
                  <BarChart
                    data={getAIPipelineData()}
                    margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis
                      dataKey="name"
                      angle={-45}
                      textAnchor="end"
                      height={60}
                    />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="count" fill="#82ca9d" name="Count" />
                  </BarChart>
                </ResponsiveContainer>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3, height: 400 }}>
                <Typography variant="h6" gutterBottom>
                  Request Success Rate
                </Typography>
                <ResponsiveContainer width="100%" height="90%">
                  <PieChart>
                    <Pie
                      data={getPerformanceData()}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) =>
                        `${name}: ${((percent as number) * 100).toFixed(0)}%`
                      }
                      outerRadius={120}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {getPerformanceData().map((entry, index) => (
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
          </>
        )}

        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Recent Images
            </Typography>
            {images.slice(0, 5).map((image) => (
              <Box
                key={image.id}
                sx={{
                  mb: 2,
                  p: 2,
                  border: "1px solid #e0e0e0",
                  borderRadius: 1,
                }}
              >
                <Typography variant="subtitle1">{image.name}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {image.dialogues.length} dialogues •{" "}
                  {image.isActive ? "Active" : "Inactive"}
                </Typography>
              </Box>
            ))}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;

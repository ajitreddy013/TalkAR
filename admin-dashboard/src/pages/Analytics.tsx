import React, { useEffect, useState } from "react";
import {
  Typography,
  Paper,
  Box,
  LinearProgress,
  Alert,
  Grid,
  Card,
  CardContent,
  Tabs,
  Tab,
} from "@mui/material";
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
  LineChart,
  Line,
  AreaChart,
  Area,
} from "recharts";
import { AnalyticsService, AnalyticsData } from "../services/analyticsService";
import { FeedbackService, FeedbackStats } from "../services/feedbackService";

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`analytics-tabpanel-${index}`}
      aria-labelledby={`analytics-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

function a11yProps(index: number) {
  return {
    id: `analytics-tab-${index}`,
    "aria-controls": `analytics-tabpanel-${index}`,
  };
}

const COLORS = ["#0088FE", "#00C49F", "#FFBB28", "#FF8042", "#8884d8"];
const FEEDBACK_COLORS = ["#4CAF50", "#F44336"]; // Green for positive, Red for negative

export default function Analytics() {
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null);
  const [feedbackStats, setFeedbackStats] = useState<FeedbackStats | null>(
    null
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tabValue, setTabValue] = useState(0);

  useEffect(() => {
    fetchAllData();
    // Auto-refresh disabled to prevent continuous page refreshes
    // const interval = setInterval(fetchAllData, 30000);
    // return () => clearInterval(interval);
  }, []);

  const fetchAllData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [analyticsResponse, feedbackResponse] = await Promise.all([
        AnalyticsService.getAnalytics(),
        FeedbackService.getFeedbackStats(),
      ]);

      setAnalytics(analyticsResponse.data.analytics);
      setFeedbackStats(feedbackResponse.data.stats);
    } catch (err) {
      console.error("Failed to fetch data:", err);
      setError("Failed to load analytics data");
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  // Prepare data for charts
  const getImageTriggerData = () => {
    if (!analytics?.imageTriggers?.byImage) return [];
    return analytics.imageTriggers.byImage.map((item) => ({
      name:
        item.imageName.length > 20
          ? `${item.imageName.substring(0, 20)}...`
          : item.imageName,
      triggers: item.count,
    }));
  };

  const getVoiceUsageData = () => {
    if (!analytics?.imageTriggers?.byVoice) return [];
    return analytics.imageTriggers.byVoice.map((item) => ({
      name: item.voiceId,
      count: item.count,
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

  const getPerformanceTrendData = () => {
    if (!analytics?.performance) return [];
    return [
      {
        name: "Response Time",
        value: analytics.performance.averageResponseTime,
      },
      {
        name: "Video Processing",
        value: analytics.performance.averageVideoProcessingTime,
      },
    ];
  };

  const getFeedbackByProductData = () => {
    if (!feedbackStats?.byProduct) return [];
    return feedbackStats.byProduct.map((item) => ({
      name: item.productName,
      positive: item.positiveCount,
      negative: item.negativeCount,
    }));
  };

  const getFeedbackOverviewData = () => {
    if (!feedbackStats) return [];
    return [
      { name: "Positive", value: feedbackStats.positive },
      { name: "Negative", value: feedbackStats.negative },
    ];
  };

  if (loading && !analytics && !feedbackStats) {
    return <LinearProgress />;
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Analytics & Feedback
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Tabs
        value={tabValue}
        onChange={handleTabChange}
        aria-label="analytics tabs"
        sx={{ mb: 3 }}
      >
        <Tab label="User Engagement" {...a11yProps(0)} />
        <Tab label="Performance" {...a11yProps(1)} />
        <Tab label="Feedback" {...a11yProps(2)} />
      </Tabs>

      <TabPanel value={tabValue} index={0}>
        <Grid container spacing={3}>
          {/* User Engagement Metrics */}
          {analytics && (
            <>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Total Image Triggers
                    </Typography>
                    <Typography variant="h4" color="primary">
                      {analytics.imageTriggers.total}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Avatar Plays
                    </Typography>
                    <Typography variant="h4" color="primary">
                      {analytics.avatarPlays.total}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      AI Pipeline Events
                    </Typography>
                    <Typography variant="h4" color="primary">
                      {analytics.aiPipelineEvents.total}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Pipeline Errors
                    </Typography>
                    <Typography variant="h4" color="error">
                      {analytics.aiPipelineEvents.errors}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            </>
          )}

          {/* Charts */}
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
                    Voice Usage
                  </Typography>
                  <ResponsiveContainer width="100%" height="90%">
                    <BarChart
                      data={getVoiceUsageData()}
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
                      <Bar dataKey="count" fill="#82ca9d" name="Usage Count" />
                    </BarChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>

              <Grid item xs={12}>
                <Paper sx={{ p: 3 }}>
                  <Typography variant="h6" gutterBottom>
                    Recent Image Triggers
                  </Typography>
                  {analytics.imageTriggers.recent.length > 0 ? (
                    <Box sx={{ maxHeight: 300, overflow: "auto" }}>
                      {analytics.imageTriggers.recent
                        .slice(0, 10)
                        .map((event) => (
                          <Box
                            key={event.id}
                            sx={{
                              mb: 2,
                              p: 2,
                              border: "1px solid #e0e0e0",
                              borderRadius: 1,
                            }}
                          >
                            <Typography variant="subtitle1">
                              {event.imageName} -{" "}
                              {new Date(event.timestamp).toLocaleString()}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              Script: "{event.scriptText.substring(0, 50)}..."
                            </Typography>
                          </Box>
                        ))}
                    </Box>
                  ) : (
                    <Typography>No recent image triggers</Typography>
                  )}
                </Paper>
              </Grid>
            </>
          )}
        </Grid>
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        <Grid container spacing={3}>
          {/* Performance Metrics */}
          {analytics && (
            <>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Avg Response Time
                    </Typography>
                    <Typography variant="h4" color="primary">
                      {analytics.performance.averageResponseTime}ms
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Success Rate
                    </Typography>
                    <Typography variant="h4" color="primary">
                      {analytics.performance.totalRequests > 0
                        ? `${Math.round(
                            (analytics.performance.successfulRequests /
                              analytics.performance.totalRequests) *
                              100
                          )}%`
                        : "0%"}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Video Processing Time
                    </Typography>
                    <Typography variant="h4" color="primary">
                      {analytics.performance.averageVideoProcessingTime}ms
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Total Requests
                    </Typography>
                    <Typography variant="h4" color="primary">
                      {analytics.performance.totalRequests}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            </>
          )}

          {/* Performance Charts */}
          {analytics && (
            <>
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
                        labelLine={true}
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
                            fill={index === 0 ? "#4CAF50" : "#F44336"}
                          />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>

              <Grid item xs={12} md={6}>
                <Paper sx={{ p: 3, height: 400 }}>
                  <Typography variant="h6" gutterBottom>
                    Performance Metrics
                  </Typography>
                  <ResponsiveContainer width="100%" height="90%">
                    <BarChart
                      data={getPerformanceTrendData()}
                      margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" />
                      <YAxis />
                      <Tooltip />
                      <Legend />
                      <Bar dataKey="value" fill="#ffc658" name="Time (ms)" />
                    </BarChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>

              <Grid item xs={12} md={6}>
                <Paper sx={{ p: 3, height: 400 }}>
                  <Typography variant="h6" gutterBottom>
                    Most Triggered Images
                  </Typography>
                  <ResponsiveContainer width="100%" height="90%">
                    <BarChart
                      data={analytics.performance.mostTriggeredImages.map(
                        (item) => ({
                          name: item.imageName,
                          triggers: item.count,
                        })
                      )}
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
            </>
          )}
        </Grid>
      </TabPanel>

      <TabPanel value={tabValue} index={2}>
        <Grid container spacing={3}>
          {/* Feedback Metrics */}
          {feedbackStats && (
            <>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Total Feedback
                    </Typography>
                    <Typography variant="h4" color="primary">
                      {feedbackStats.total}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Positive Feedback
                    </Typography>
                    <Typography variant="h4" color="success.main">
                      {feedbackStats.positive} (
                      {feedbackStats.positivePercentage}%)
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Negative Feedback
                    </Typography>
                    <Typography variant="h4" color="error.main">
                      {feedbackStats.negative} (
                      {feedbackStats.negativePercentage}%)
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Feedback Score
                    </Typography>
                    <Typography variant="h4" color="primary">
                      {feedbackStats.total > 0
                        ? `${Math.round(
                            (feedbackStats.positive / feedbackStats.total) * 100
                          )}%`
                        : "0%"}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            </>
          )}

          {/* Feedback Charts */}
          {feedbackStats && (
            <>
              <Grid item xs={12} md={6}>
                <Paper sx={{ p: 3, height: 400 }}>
                  <Typography variant="h6" gutterBottom>
                    Feedback Overview
                  </Typography>
                  <ResponsiveContainer width="100%" height="90%">
                    <PieChart>
                      <Pie
                        data={getFeedbackOverviewData()}
                        cx="50%"
                        cy="50%"
                        labelLine={true}
                        label={({ name, percent }) =>
                          `${name}: ${((percent as number) * 100).toFixed(0)}%`
                        }
                        outerRadius={120}
                        fill="#8884d8"
                        dataKey="value"
                      >
                        {getFeedbackOverviewData().map((entry, index) => (
                          <Cell
                            key={`cell-${index}`}
                            fill={index === 0 ? "#4CAF50" : "#F44336"}
                          />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>

              <Grid item xs={12} md={6}>
                <Paper sx={{ p: 3, height: 400 }}>
                  <Typography variant="h6" gutterBottom>
                    Feedback by Product
                  </Typography>
                  <ResponsiveContainer width="100%" height="90%">
                    <BarChart
                      data={getFeedbackByProductData()}
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
                      <Bar dataKey="positive" fill="#4CAF50" name="Positive" />
                      <Bar dataKey="negative" fill="#F44336" name="Negative" />
                    </BarChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>

              <Grid item xs={12}>
                <Paper sx={{ p: 3 }}>
                  <Typography variant="h6" gutterBottom>
                    Product Feedback Scores
                  </Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <AreaChart
                      data={feedbackStats.byProduct.map((item) => ({
                        name: item.productName,
                        score: item.positivePercentage,
                      }))}
                      margin={{ top: 10, right: 30, left: 0, bottom: 60 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis
                        dataKey="name"
                        angle={-45}
                        textAnchor="end"
                        height={60}
                      />
                      <YAxis domain={[0, 100]} />
                      <Tooltip
                        formatter={(value) => [`${value}%`, "Positive Score"]}
                      />
                      <Legend />
                      <Area
                        type="monotone"
                        dataKey="score"
                        stroke="#8884d8"
                        fill="#8884d8"
                        fillOpacity={0.3}
                        name="Positive Score %"
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>
            </>
          )}
        </Grid>
      </TabPanel>
    </Box>
  );
}

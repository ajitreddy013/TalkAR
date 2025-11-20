-- Create interactions table
CREATE TABLE IF NOT EXISTS interactions (
  id BIGSERIAL PRIMARY KEY,
  user_id TEXT,
  poster_id TEXT NOT NULL,
  script TEXT,
  audio_url TEXT,
  video_url TEXT,
  feedback TEXT,
  job_id TEXT,
  status TEXT,
  latency_ms INT,
  created_at TIMESTAMP DEFAULT now()
);

-- Create metrics table
CREATE TABLE IF NOT EXISTS metrics (
  id BIGSERIAL PRIMARY KEY,
  date DATE NOT NULL,
  scans INT DEFAULT 0,
  plays INT DEFAULT 0,
  avg_latency_ms FLOAT DEFAULT 0,
  likes INT DEFAULT 0,
  dislikes INT DEFAULT 0
);

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_interactions_created_at ON interactions(created_at);
CREATE INDEX IF NOT EXISTS idx_metrics_date ON metrics(date);

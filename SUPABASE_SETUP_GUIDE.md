# Supabase Integration Setup Guide

This guide will help you set up and test the complete Supabase integration for the TalkAR project.

## Prerequisites

1. **Supabase Account**: Sign up at [supabase.com](https://supabase.com)
2. **Supabase Project**: Create a new project in your Supabase dashboard
3. **Environment Variables**: You'll need your Supabase credentials

## Step 1: Get Your Supabase Credentials

1. Go to your Supabase project dashboard
2. Navigate to **Settings** → **API**
3. Copy the following values:
   - **Project URL** (e.g., `https://your-project.supabase.co`)
   - **anon public** key (for client-side)
   - **service_role** key (for server-side - keep this secret!)

## Step 2: Configure Environment Variables

### Backend Configuration

Create a `.env` file in the `backend` directory with:

```env
# Database (existing)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=talkar
DB_USER=your_db_user
DB_PASSWORD=your_db_password

# Server (existing)
PORT=3000
NODE_ENV=development

# JWT (existing)
JWT_SECRET=your_jwt_secret_key

# Supabase (NEW)
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your_anon_key
SUPABASE_SERVICE_KEY=your_service_role_key

# Sync API (existing)
SYNC_API_URL=https://api.sync.so/v2
SYNC_API_KEY=your_sync_api_key

# AWS (existing)
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_REGION=us-east-1
S3_BUCKET_NAME=talkar-uploads

# CORS (existing)
CORS_ORIGIN=http://localhost:3000,http://localhost:3001

# File Upload (existing)
MAX_FILE_SIZE=10485760
ALLOWED_FILE_TYPES=jpg,jpeg,png,gif,mp4,avi,mov
```

### Frontend Configuration

Create a `.env` file in the `admin-dashboard` directory with:

```env
# Supabase (NEW)
REACT_APP_SUPABASE_URL=https://your-project.supabase.co
REACT_APP_SUPABASE_ANON_KEY=your_anon_key

# API (existing)
REACT_APP_API_URL=http://localhost:3000/api/v1

# File Upload (existing)
REACT_APP_MAX_FILE_SIZE=10485760
REACT_APP_ALLOWED_FILE_TYPES=jpg,jpeg,png,gif,mp4,avi,mov
```

## Step 3: Set Up Supabase Database Schema

1. Go to your Supabase project dashboard
2. Navigate to **SQL Editor**
3. Run the following SQL to create the tables:

```sql
-- Create user_profiles table
CREATE TABLE user_profiles (
  id UUID PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  full_name TEXT,
  avatar_url TEXT,
  role TEXT DEFAULT 'user',
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create projects table
CREATE TABLE projects (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT,
  status TEXT DEFAULT 'active',
  settings JSONB DEFAULT '{}',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create sync_jobs table
CREATE TABLE sync_jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
  project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
  text TEXT NOT NULL,
  language TEXT DEFAULT 'en',
  voice_id TEXT,
  image_url TEXT,
  video_url TEXT,
  duration INTEGER,
  status TEXT DEFAULT 'pending',
  error TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security (RLS)
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE sync_jobs ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
CREATE POLICY "Users can view own profile" ON user_profiles
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" ON user_profiles
  FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Users can view own projects" ON projects
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can create own projects" ON projects
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own projects" ON projects
  FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own projects" ON projects
  FOR DELETE USING (auth.uid() = user_id);

CREATE POLICY "Users can view own sync jobs" ON sync_jobs
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can create own sync jobs" ON sync_jobs
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Create updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_profiles_updated_at
  BEFORE UPDATE ON user_profiles
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_projects_updated_at
  BEFORE UPDATE ON projects
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sync_jobs_updated_at
  BEFORE UPDATE ON sync_jobs
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create indexes for performance
CREATE INDEX idx_user_profiles_email ON user_profiles(email);
CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_sync_jobs_user_id ON sync_jobs(user_id);
CREATE INDEX idx_sync_jobs_project_id ON sync_jobs(project_id);
CREATE INDEX idx_sync_jobs_status ON sync_jobs(status);
```

## Step 4: Install Dependencies

### Backend Dependencies

```bash
cd backend
npm install @supabase/supabase-js
```

### Frontend Dependencies

```bash
cd admin-dashboard
npm install @supabase/supabase-js
```

## Step 5: Run the Tests

### Test 1: Supabase Connection Test

```bash
cd backend
node test-supabase-integration.js
```

This will test:
- Connection to Supabase
- User authentication
- User profile operations
- Project operations
- Sync job operations
- Real-time subscriptions

### Test 2: Backend API Test

First, start the backend server with Supabase integration:

```bash
cd backend
# Use the Supabase version of the server
node -r ts-node/register src/indexSupabase.ts
```

In another terminal, run the API tests:

```bash
cd backend
node test-backend-api-supabase.js
```

### Test 3: Frontend Integration Test

```bash
cd admin-dashboard
node test-supabase-frontend.js
```

## Step 6: Run the Application

### Start Backend Server

```bash
cd backend
npm run dev
# or for Supabase version:
node -r ts-node/register src/indexSupabase.ts
```

### Start Frontend

```bash
cd admin-dashboard
npm start
```

## Features Now Available

### Authentication
- ✅ User registration with email/password
- ✅ User login with JWT tokens
- ✅ Password change functionality
- ✅ User profile management

### Database Operations
- ✅ User profiles stored in Supabase
- ✅ Project management with user associations
- ✅ Sync job tracking and status updates
- ✅ Real-time data synchronization

### Real-time Features
- ✅ Live project updates
- ✅ Real-time sync job status
- ✅ User-specific data subscriptions

### File Management
- ✅ File uploads to Supabase Storage
- ✅ Image and video file handling
- ✅ Secure file access with RLS policies

## Troubleshooting

### Connection Issues
- Verify your Supabase URL and keys are correct
- Check that your Supabase project is active
- Ensure network connectivity to Supabase

### Authentication Issues
- Make sure RLS policies are properly configured
- Verify JWT secret matches between backend and Supabase
- Check user roles and permissions

### Database Issues
- Ensure all tables are created with correct schema
- Verify RLS policies are enabled
- Check for proper indexes on frequently queried columns

### Real-time Issues
- Enable Realtime for your tables in Supabase dashboard
- Check network connectivity for WebSocket connections
- Verify subscription filters are correct

## Next Steps

1. **Deploy to Production**: Update your production environment variables
2. **Custom Domain**: Configure custom domain for Supabase
3. **Scaling**: Monitor usage and scale as needed
4. **Security**: Review and tighten security policies
5. **Monitoring**: Set up monitoring and alerting

## Support

For issues with Supabase integration:
1. Check the [Supabase Documentation](https://supabase.com/docs)
2. Review the test output for specific error messages
3. Verify all environment variables are set correctly
4. Ensure your Supabase project is properly configured

For TalkAR-specific issues:
1. Check the existing documentation in `/docs`
2. Review the API documentation for endpoint details
3. Test with the provided test scripts before manual testing
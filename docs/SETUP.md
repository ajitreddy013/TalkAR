# TalkAR Setup Guide

## Prerequisites

- Node.js 18+ and npm
- Android Studio (for mobile app development)
- Docker and Docker Compose (for backend services)
- PostgreSQL (or use Docker)
- Supabase account (for authentication and file storage)
- Sync API account (for lip-sync generation)

## 🚀 Quick Start with Supabase Integration

**NEW: Complete Supabase integration is now available!**

For comprehensive Supabase setup instructions, see: [SUPABASE_FINAL_INTEGRATION_GUIDE.md](SUPABASE_FINAL_INTEGRATION_GUIDE.md)

This integration provides:
- Complete user authentication system
- File storage and management
- Real-time data synchronization
- Comprehensive API with full CRUD operations
- Mobile AR component integration
- Performance monitoring and testing

## Quick Start with Docker

1. **Clone the repository:**

   ```bash
   git clone <repository-url>
   cd TalkAR
   ```

2. **Set up environment variables:**

   ```bash
   cp backend/env.example backend/.env
   # Edit backend/.env with your configuration
   ```

3. **Start services with Docker Compose:**

   ```bash
   docker-compose up -d
   ```

4. **Access the applications:**
   - Backend API: http://localhost:3000
   - Admin Dashboard: http://localhost:3001
   - Database: localhost:5432

## Manual Setup

### Backend Setup

1. **Navigate to backend directory:**

   ```bash
   cd backend
   ```

2. **Install dependencies:**

   ```bash
   npm install
   ```

3. **Set up environment variables:**

   ```bash
   cp env.example .env
   # Edit .env with your configuration
   ```

4. **Set up database:**

   ```bash
   # Create PostgreSQL database
   createdb talkar_db
   ```

5. **Run migrations:**
   ```bash
   npm run build
   npm start
   ```

### Admin Dashboard Setup

1. **Navigate to admin dashboard directory:**

   ```bash
   cd admin-dashboard
   ```

2. **Install dependencies:**

   ```bash
   npm install
   ```

3. **Set up environment variables:**

   ```bash
   # Create .env file
   echo "REACT_APP_API_URL=http://localhost:3000/api/v1" > .env
   ```

4. **Start development server:**
   ```bash
   npm start
   ```

### Mobile App Setup

1. **Open Android Studio**

2. **Import the project:**

   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the `mobile-app` directory

3. **Configure the app:**

   - Update `ApiClient.kt` with your backend URL
   - Add your Sync API key

4. **Build and run:**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio

## Configuration

### Environment Variables

#### Backend (.env)

```env
# Database
DATABASE_URL=postgresql://username:password@localhost:5432/talkar_db
DB_HOST=localhost
DB_PORT=5432
DB_NAME=talkar_db
DB_USER=username
DB_PASSWORD=password

# Server
PORT=3000
NODE_ENV=development

# JWT
JWT_SECRET=your-super-secret-jwt-key
JWT_EXPIRES_IN=7d

# AWS S3
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
AWS_REGION=us-east-1
AWS_S3_BUCKET=talkar-assets

# Sync API
SYNC_API_URL=https://api.sync.com/v1
SYNC_API_KEY=your-sync-api-key

# CORS
CORS_ORIGIN=http://localhost:3001
```

#### Admin Dashboard (.env)

```env
REACT_APP_API_URL=http://localhost:3000/api/v1
```

### Mobile App Configuration

Update the API base URL in `mobile-app/app/src/main/java/com/talkar/app/data/api/ApiClient.kt`:

```kotlin
private const val BASE_URL = "https://your-api-domain.com/v1/"
```

## Development

### Backend Development

```bash
cd backend
npm run dev  # Start with hot reload
```

### Admin Dashboard Development

```bash
cd admin-dashboard
npm start  # Start React development server
```

### Mobile App Development

1. Open the project in Android Studio
2. Use the built-in development tools
3. Enable USB debugging on your device

## Testing

### Backend Tests

```bash
cd backend
npm test
```

### Admin Dashboard Tests

```bash
cd admin-dashboard
npm test
```

## Deployment

### Backend Deployment

1. **Build the application:**

   ```bash
   npm run build
   ```

2. **Deploy to your server:**
   - Upload the `dist` folder
   - Install dependencies: `npm ci --production`
   - Start with PM2: `pm2 start dist/index.js`

### Admin Dashboard Deployment

1. **Build the application:**

   ```bash
   npm run build
   ```

2. **Deploy the `build` folder to your web server**

### Mobile App Deployment

1. **Generate signed APK:**

   - Build → Generate Signed Bundle/APK
   - Follow Android Studio prompts

2. **Upload to Google Play Store**

## Troubleshooting

### Common Issues

1. **Database connection failed:**

   - Check PostgreSQL is running
   - Verify connection credentials
   - Ensure database exists

2. **AWS S3 upload failed:**

   - Verify AWS credentials
   - Check S3 bucket permissions
   - Ensure bucket exists

3. **Sync API errors:**

   - Verify API key is correct
   - Check API endpoint URL
   - Ensure sufficient API credits

4. **Mobile app build errors:**
   - Check Android SDK version
   - Verify ARCore is properly configured
   - Update dependencies if needed

### Getting Help

- Check the logs in each service
- Review the API documentation
- Check the mobile app logs in Android Studio

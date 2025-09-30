# TalkAR Development Environments Setup

This guide provides detailed instructions for setting up all development environments required for the TalkAR project.

## 🎯 Overview

TalkAR requires three main development environments:

1. **Android Studio** - Mobile app development
2. **Node.js** - Backend API development
3. **React** - Admin dashboard development
4. **Firebase/Firestore** - Database and authentication

## 📱 Android Studio Setup

### Prerequisites

- **Operating System**: Windows 10+, macOS 10.14+, or Linux Ubuntu 18.04+
- **RAM**: Minimum 8GB, recommended 16GB
- **Storage**: At least 10GB free space
- **Java**: OpenJDK 11 or Oracle JDK 11

### Installation Steps

#### 1. Download Android Studio

```bash
# Visit: https://developer.android.com/studio
# Download the latest stable version
```

#### 2. Install Android Studio

- **Windows**: Run the installer as administrator
- **macOS**: Drag to Applications folder
- **Linux**: Extract and run `./studio.sh`

#### 3. Initial Setup Wizard

1. **Welcome Screen**: Choose "Standard" installation
2. **SDK Components**: Accept default selections
3. **Verify Settings**: Review and finish setup

#### 4. Configure ARCore Support

```bash
# In Android Studio:
# 1. Go to Tools → SDK Manager
# 2. SDK Tools tab
# 3. Check "ARCore SDK for Android"
# 4. Click Apply and install
```

#### 5. Install Required SDKs

```bash
# SDK Platforms:
# - Android 14 (API 34) - Target SDK
# - Android 7.0 (API 24) - Minimum SDK
# - Android 13 (API 33) - Recommended

# SDK Tools:
# - Android SDK Build-Tools 34.0.0
# - Android SDK Platform-Tools
# - Android SDK Tools
# - ARCore SDK for Android
```

#### 6. Configure Environment Variables

```bash
# Add to your shell profile (.bashrc, .zshrc, etc.)
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
```

#### 7. Test Installation

```bash
# Open Android Studio
# Create new project → "Empty Activity"
# Build and run on emulator
```

### ARCore Specific Configuration

#### 1. Enable ARCore in Project

```gradle
// In app/build.gradle
dependencies {
    implementation 'com.google.ar:core:1.41.0'
    implementation 'com.google.ar.sceneform:filament-android:1.17.1'
    implementation 'com.google.ar.sceneform:sceneform-base:1.17.1'
    implementation 'com.google.ar.sceneform:sceneform-core:1.17.1'
    implementation 'com.google.ar.sceneform:sceneform-filament:1.17.1'
    implementation 'com.google.ar.sceneform:sceneform-ux:1.17.1'
}
```

#### 2. Update AndroidManifest.xml

```xml
<!-- Add ARCore permissions -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.ar" android:required="true" />

<!-- Add ARCore meta-data -->
<meta-data android:name="com.google.ar.core" android:value="required" />
```

#### 3. Test ARCore Setup

```bash
# Create ARCore test activity
# Verify camera permissions
# Test on physical device (ARCore requires real device)
```

## 🖥️ Node.js Backend Setup

### Prerequisites

- **Node.js**: Version 18.0.0 or higher
- **npm**: Version 8.0.0 or higher (comes with Node.js)
- **PostgreSQL**: Version 13 or higher
- **Git**: For version control

### Installation Steps

#### 1. Install Node.js

```bash
# Option 1: Official installer
# Visit: https://nodejs.org/
# Download LTS version (18.x or higher)

# Option 2: Using package manager
# macOS (Homebrew):
brew install node

# Ubuntu/Debian:
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Windows (Chocolatey):
choco install nodejs
```

#### 2. Verify Installation

```bash
node --version    # Should show v18.x.x or higher
npm --version     # Should show 8.x.x or higher
```

#### 3. Install PostgreSQL

```bash
# macOS (Homebrew):
brew install postgresql
brew services start postgresql

# Ubuntu/Debian:
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Windows:
# Download from: https://www.postgresql.org/download/windows/
```

#### 4. Configure PostgreSQL

```bash
# Create database user
sudo -u postgres createuser --interactive
# Enter username: talkar_user
# Enter role: y (superuser)

# Create database
sudo -u postgres createdb talkar_db

# Set password
sudo -u postgres psql
ALTER USER talkar_user PASSWORD 'your_password';
\q
```

#### 5. Install Project Dependencies

```bash
cd backend
npm install
```

#### 6. Environment Configuration

```bash
# Copy environment template
cp env.example .env

# Edit .env file with your configuration
nano .env
```

#### 7. Database Setup

```bash
# Run database migrations (if available)
npm run migrate

# Or start the application (will create tables)
npm run dev
```

#### 8. Test Backend

```bash
# Start development server
npm run dev

# Test API endpoint
curl http://localhost:3000/api/v1/health
```

## ⚛️ React Dashboard Setup

### Prerequisites

- **Node.js**: Version 18.0.0 or higher (already installed)
- **Modern Browser**: Chrome, Firefox, Safari, or Edge

### Installation Steps

#### 1. Navigate to Dashboard Directory

```bash
cd admin-dashboard
```

#### 2. Install Dependencies

```bash
npm install
```

#### 3. Environment Configuration

```bash
# Create .env file
echo "REACT_APP_API_URL=http://localhost:3000/api/v1" > .env

# For production, update with your API URL
echo "REACT_APP_API_URL=https://your-api-domain.com/api/v1" > .env
```

#### 4. Start Development Server

```bash
npm start
```

#### 5. Access Dashboard

```bash
# Open browser to: http://localhost:3001
# The app will automatically reload when you make changes
```

#### 6. Test Dashboard Features

- Verify API connection
- Test image upload functionality
- Check dialogue management
- Verify analytics display

## 🔥 Firebase/Firestore Setup

### Prerequisites

- **Google Account**: For Firebase console access
- **Firebase CLI**: For local development
- **Node.js**: Already installed

### Installation Steps

#### 1. Create Firebase Project

```bash
# Visit: https://console.firebase.google.com/
# Click "Create a project"
# Enter project name: "talkar-app"
# Enable Google Analytics (optional)
# Choose Analytics account
# Click "Create project"
```

#### 2. Enable Firestore Database

```bash
# In Firebase Console:
# 1. Go to "Firestore Database"
# 2. Click "Create database"
# 3. Choose "Start in test mode" (for development)
# 4. Select location (closest to your users)
# 5. Click "Done"
```

#### 3. Configure Authentication

```bash
# In Firebase Console:
# 1. Go to "Authentication"
# 2. Click "Get started"
# 3. Go to "Sign-in method" tab
# 4. Enable "Email/Password"
# 5. Enable "Google" (optional)
```

#### 4. Install Firebase CLI

```bash
npm install -g firebase-tools
```

#### 5. Login to Firebase

```bash
firebase login
```

#### 6. Initialize Firebase in Project

```bash
# In your project root
firebase init

# Select features:
# - Firestore
# - Functions (optional)
# - Hosting (optional)

# Choose your Firebase project
# Configure Firestore rules
# Configure functions (if selected)
```

#### 7. Configure Environment Variables

```bash
# Add to backend/.env
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY=your-private-key
FIREBASE_CLIENT_EMAIL=your-client-email

# Add to admin-dashboard/.env
REACT_APP_FIREBASE_API_KEY=your-api-key
REACT_APP_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
REACT_APP_FIREBASE_PROJECT_ID=your-project-id
REACT_APP_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
REACT_APP_FIREBASE_MESSAGING_SENDER_ID=your-sender-id
REACT_APP_FIREBASE_APP_ID=your-app-id
```

#### 8. Test Firebase Connection

```bash
# Test Firestore connection
firebase firestore:get /test

# Test authentication (in your app)
# Verify user can sign in/out
```

## 🐳 Docker Development Setup

### Prerequisites

- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher

### Installation Steps

#### 1. Install Docker

```bash
# macOS: Download Docker Desktop from https://docker.com
# Windows: Download Docker Desktop from https://docker.com
# Ubuntu:
sudo apt update
sudo apt install docker.io docker-compose
sudo systemctl start docker
sudo systemctl enable docker
```

#### 2. Verify Installation

```bash
docker --version
docker-compose --version
```

#### 3. Start Development Environment

```bash
# In project root
docker-compose up -d
```

#### 4. Access Services

```bash
# Backend API: http://localhost:3000
# Admin Dashboard: http://localhost:3001
# Database: localhost:5432
```

## 🔧 Development Tools

### Recommended VS Code Extensions

```json
{
  "recommendations": [
    "ms-vscode.vscode-typescript-next",
    "bradlc.vscode-tailwindcss",
    "esbenp.prettier-vscode",
    "ms-vscode.vscode-eslint",
    "ms-vscode.vscode-json",
    "redhat.vscode-yaml",
    "ms-vscode.vscode-docker"
  ]
}
```

### Android Studio Plugins

- **Kotlin**: Built-in support
- **ARCore**: For AR development
- **Git Integration**: Built-in support
- **Database Inspector**: For debugging

### Browser Extensions

- **React Developer Tools**: For React debugging
- **Redux DevTools**: For state management
- **Network Monitor**: For API debugging

## 🧪 Testing Setup

### Backend Testing

```bash
cd backend
npm test
```

### Frontend Testing

```bash
cd admin-dashboard
npm test
```

### Mobile App Testing

```bash
# In Android Studio
# Run → Edit Configurations
# Add JUnit test configuration
```

## 🚀 Production Deployment

### Backend Deployment

```bash
# Build for production
npm run build

# Deploy to server
# Use PM2 for process management
pm2 start dist/index.js --name talkar-backend
```

### Frontend Deployment

```bash
# Build for production
npm run build

# Deploy build folder to hosting service
# Netlify, Vercel, or AWS S3
```

### Mobile App Deployment

```bash
# Generate signed APK
# Build → Generate Signed Bundle/APK
# Upload to Google Play Store
```

## 🔍 Troubleshooting

### Common Issues

#### Android Studio

- **ARCore not working**: Ensure physical device with ARCore support
- **Build errors**: Check SDK versions and dependencies
- **Emulator issues**: Use physical device for AR testing

#### Node.js Backend

- **Database connection**: Verify PostgreSQL is running
- **Port conflicts**: Check if port 3000 is available
- **Environment variables**: Ensure .env file is configured

#### React Dashboard

- **API connection**: Verify backend is running
- **CORS issues**: Check backend CORS configuration
- **Build errors**: Clear node_modules and reinstall

#### Firebase

- **Authentication**: Check API keys and configuration
- **Firestore rules**: Ensure proper security rules
- **CLI issues**: Update Firebase CLI to latest version

### Getting Help

- Check project documentation
- Review error logs
- Search GitHub issues
- Contact development team

## 📚 Additional Resources

- [Android Studio Documentation](https://developer.android.com/studio)
- [ARCore Documentation](https://developers.google.com/ar)
- [Node.js Documentation](https://nodejs.org/docs)
- [React Documentation](https://reactjs.org/docs)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Docker Documentation](https://docs.docker.com/)

This comprehensive setup guide ensures all development environments are properly configured for the TalkAR project.

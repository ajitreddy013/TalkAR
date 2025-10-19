# TalkAR

[![Security](https://github.com/ajitreddy013/TalkAR/actions/workflows/security.yml/badge.svg)](https://github.com/ajitreddy013/TalkAR/actions/workflows/security.yml)

## Project Overview

TalkAR is an innovative augmented reality application that enables interactive storytelling through animated avatars. The app uses advanced computer vision to track images and overlay dynamic 3D avatars with synchronized lip-sync animations, creating an immersive storytelling experience.

## Security

This repository has security measures in place to prevent secret leaks:

- Pre-commit hooks to detect secrets before committing
- GitHub secret scanning enabled
- Regular security audits

For security guidelines, see [SECURITY_GUIDELINES.md](SECURITY_GUIDELINES.md).

## 🎯 App Vision

TalkAR revolutionizes how users interact with static images by bringing them to life through AR technology. When users point their camera at recognized images (posters, advertisements, educational materials), the app generates realistic talking head avatars that speak in their preferred language, creating engaging and personalized experiences.

### Core Value Proposition

- **Immersive AR Experience**: Transform static images into interactive talking heads
- **Multi-language Support**: Break language barriers with real-time translation and voice synthesis
- **Educational & Marketing Applications**: Perfect for museums, retail, education, and advertising
- **Accessibility**: Make content accessible to users with different language preferences

## 🏗️ Project Structure

```
TalkAR/
├── mobile-app/          # Android AR app (Kotlin + ARCore)
│   ├── app/
│   │   ├── src/main/java/com/talkar/app/
│   │   │   ├── data/           # Data layer (API, local DB)
│   │   │   ├── ui/             # UI components and screens
│   │   │   └── viewmodels/     # Business logic
│   │   └── build.gradle        # ARCore + Compose setup
│   └── build.gradle
├── backend/            # Node.js/TypeScript API
│   ├── src/
│   │   ├── config/     # Database configuration
│   │   ├── middleware/ # Auth, validation, error handling
│   │   ├── models/      # Data models
│   │   ├── routes/      # API endpoints
│   │   └── services/    # Business logic services
│   └── package.json
├── admin-dashboard/    # React TypeScript dashboard
│   ├── src/
│   │   ├── components/ # Reusable UI components
│   │   ├── pages/      # Dashboard pages
│   │   ├── services/   # API integration
│   │   └── store/      # Redux state management
│   └── package.json
├── docs/              # Comprehensive documentation
│   ├── API.md         # API documentation
│   └── SETUP.md       # Development setup guide
└── docker-compose.yml # Container orchestration
```

## ✨ Key Features

### Mobile App (Android)

- **AR Image Recognition**: ARCore-powered image detection and tracking
- **Real-time AR Overlay**: Seamless 3D talking head rendering
- **Multi-language Support**: Dynamic language switching
- **Offline Capability**: Cached content for offline usage
- **Modern UI**: Jetpack Compose with Material Design 3

### Backend API

- **RESTful API**: Comprehensive endpoints for image and dialogue management
- **Sync API Integration**: Lip-sync video generation
- **Cloud Storage**: AWS S3 integration for media assets
- **Authentication**: JWT-based secure access
- **Analytics**: Usage tracking and insights

### Admin Dashboard

- **Content Management**: Upload and manage images and dialogues
- **Analytics Dashboard**: Usage statistics and insights
- **Multi-language Editor**: Manage content in multiple languages
- **User Management**: Admin access control

## 🛠️ Tech Stack

### Mobile App (Android)

- **Language**: Kotlin
- **AR Framework**: ARCore 1.41.0
- **UI Framework**: Jetpack Compose
- **Networking**: Retrofit 2.9.0 + OkHttp
- **Image Loading**: Coil 2.5.0
- **Architecture**: MVVM with Repository pattern
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

### Backend API

- **Runtime**: Node.js 18+
- **Language**: TypeScript 5.3.3
- **Framework**: Express.js 4.18.2
- **Database**: PostgreSQL with Sequelize ORM
- **Authentication**: JWT with bcryptjs
- **File Storage**: AWS S3 SDK
- **Validation**: Joi 17.11.0
- **Security**: Helmet, CORS

### Admin Dashboard

- **Framework**: React 18.2.0 + TypeScript
- **UI Library**: Material-UI 5.15.0
- **State Management**: Redux Toolkit 2.0.1
- **Routing**: React Router DOM 6.20.1
- **Forms**: React Hook Form 7.48.2
- **HTTP Client**: Axios 1.6.2

### External Services

- **Sync API**: Lip-sync video generation
- **AWS S3**: Cloud storage for images and videos
- **PostgreSQL**: Primary database
- **Docker**: Containerization and orchestration

## 🚀 Quick Start

### Prerequisites

- Node.js 18+ and npm
- Android Studio (latest version)
- Docker and Docker Compose
- PostgreSQL (or use Docker)
- AWS S3 account
- Sync API account

### 1. Clone and Setup

```bash
git clone <repository-url>
cd TalkAR
```

### 2. Environment Configuration

```bash
# Backend environment
cp backend/env.example backend/.env
# Edit backend/.env with your configuration

# Admin dashboard environment
echo "REACT_APP_API_URL=http://localhost:3000/api/v1" > admin-dashboard/.env
```

### 3. Start with Docker (Recommended)

```bash
docker-compose up -d
```

### 4. Manual Development Setup

```bash
# Backend
cd backend && npm install && npm run dev

# Admin Dashboard
cd admin-dashboard && npm install && npm start

# Mobile App
# Open mobile-app/ in Android Studio
```

## 📱 Development Environments

### Android Studio Setup

1. **Install Android Studio** (latest version)
2. **Configure ARCore**:
   - Install ARCore SDK
   - Enable ARCore in project settings
   - Configure device for AR testing
3. **Import Project**: Open `mobile-app/` directory
4. **Sync Dependencies**: Gradle sync will install ARCore and Compose dependencies

### Node.js Backend Setup

1. **Install Node.js 18+**
2. **Install Dependencies**: `npm install`
3. **Database Setup**: Configure PostgreSQL connection
4. **Environment Variables**: Set up `.env` file
5. **Development Server**: `npm run dev`

### React Dashboard Setup

1. **Install Dependencies**: `npm install`
2. **Environment Configuration**: Set API URL
3. **Development Server**: `npm start`
4. **Access**: http://localhost:3001

### Firebase/Firestore Setup

1. **Create Firebase Project**
2. **Enable Firestore Database**
3. **Configure Authentication**
4. **Update Environment Variables**

## 🔧 API Integration

### Sync API

- **Purpose**: Generate lip-synced talking head videos
- **Integration**: RESTful API calls for video generation
- **Authentication**: API key-based access
- **Features**: Multiple voice options, language support

### ARCore Integration

- **Image Tracking**: Recognize and track reference images
- **3D Rendering**: Overlay talking head models
- **Performance**: Optimized for mobile devices
- **Compatibility**: Android 7.0+ with ARCore support

### Cloud Storage

- **AWS S3**: Store images, videos, and assets
- **CDN Integration**: Fast content delivery
- **Security**: Secure access controls

## 📚 Documentation

- **[Setup Guide](docs/SETUP.md)**: Comprehensive development setup
- **[API Documentation](docs/API.md)**: Complete API reference
- **Architecture**: MVVM pattern with clean separation
- **Deployment**: Docker-based containerization

## 🎯 Phase 1 Deliverables ✅

- ✅ **Repository Structure**: Monorepo with organized folders
- ✅ **Tech Stack Definition**: Complete technology specifications
- ✅ **Development Environments**: Setup guides for all platforms
- ✅ **Documentation**: Comprehensive README and setup guides
- ✅ **Project Architecture**: Clean, scalable code organization

## 🚀 Enhanced AR Development Roadmap

### Current Status (Week 1)

- ✅ **TalkAR Enhanced Development Branch**: Created and ready
- ✅ **AR Components**: Multiple AR implementations ready for testing
- ✅ **Backend Integration**: Complete API system functional
- ✅ **Device Testing**: Samsung A35 ready for development

### Week 1 Deliverables

- [ ] **Device Setup**: Samsung A35 connected and tested
- [ ] **AR Component Testing**: All existing AR components verified
- [ ] **Performance Baseline**: AR performance metrics established
- [ ] **Documentation**: Enhanced AR development roadmap documented

### Upcoming Enhanced Features (Weeks 2-8)

- **3D Avatar System**: Interactive 3D avatars with full body movement
- **Voice Commands**: Natural language processing and voice recognition
- **Product Interaction**: AR shopping and product manipulation
- **High-Fidelity Rendering**: Photorealistic 3D rendering with advanced lighting
- **Mobile Optimization**: Enhanced ARCore stability and offline caching

## 🔄 Next Steps

Ready for Enhanced AR Development Phase

- Test existing AR components on Samsung A35
- Implement 3D avatar system
- Add voice command integration
- Develop product interaction features
- Optimize AR performance for production

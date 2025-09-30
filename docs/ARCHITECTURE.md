# TalkAR Architecture Documentation

## 📁 Detailed Folder Structure

This document provides a comprehensive overview of the TalkAR project structure, explaining the purpose and organization of each component.

## 🏗️ Project Overview

TalkAR is organized as a monorepo containing three main applications:

- **Mobile App**: Android AR application
- **Backend**: Node.js/TypeScript API server
- **Admin Dashboard**: React TypeScript web application

## 📱 Mobile App Structure (`mobile-app/`)

```
mobile-app/
├── app/                           # Main Android application module
│   ├── build.gradle              # App-level build configuration
│   ├── proguard-rules.pro        # Code obfuscation rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml    # App permissions and configuration
│           ├── java/com/talkar/app/    # Kotlin source code
│           │   ├── data/              # Data layer
│           │   │   ├── api/           # Network API clients
│           │   │   │   └── ApiClient.kt
│           │   │   ├── local/         # Local database
│           │   │   │   ├── ImageDao.kt
│           │   │   │   └── ImageDatabase.kt
│           │   │   ├── models/        # Data models
│           │   │   │   └── ImageRecognition.kt
│           │   │   └── repository/    # Repository pattern
│           │   │       ├── ImageRepository.kt
│           │   │       └── SyncRepository.kt
│           │   ├── ui/                # User interface
│           │   │   ├── components/    # Reusable UI components
│           │   │   │   ├── ARView.kt
│           │   │   │   ├── ImageRecognitionCard.kt
│           │   │   │   └── SyncVideoPlayer.kt
│           │   │   ├── screens/       # App screens
│           │   │   │   └── ARScreen.kt
│           │   │   ├── theme/         # UI theming
│           │   │   │   ├── Color.kt
│           │   │   │   ├── Theme.kt
│           │   │   │   └── Type.kt
│           │   │   └── viewmodels/     # Business logic
│           │   │       └── ARViewModel.kt
│           │   ├── MainActivity.kt     # Main activity entry point
│           │   └── TalkARApplication.kt # Application class
│           └── res/                    # Android resources
│               └── values/
│                   ├── colors.xml     # Color definitions
│                   ├── strings.xml    # String resources
│                   └── themes.xml     # Theme definitions
├── build.gradle                      # Project-level build configuration
├── gradle.properties                # Gradle properties
└── settings.gradle                  # Gradle settings
```

### Mobile App Architecture

**Data Layer (`data/`)**

- **API**: Network communication with backend services
- **Local**: Room database for offline storage
- **Models**: Data transfer objects and entities
- **Repository**: Abstraction layer between data sources

**UI Layer (`ui/`)**

- **Components**: Reusable Jetpack Compose components
- **Screens**: Main app screens and navigation
- **Theme**: Material Design 3 theming system
- **ViewModels**: MVVM pattern business logic

**Key Technologies**

- **ARCore**: Google's AR framework for image recognition
- **Jetpack Compose**: Modern declarative UI toolkit
- **Retrofit**: Type-safe HTTP client
- **Room**: Local database for offline support

## 🖥️ Backend Structure (`backend/`)

```
backend/
├── src/
│   ├── config/                      # Configuration files
│   │   └── database.ts              # Database connection setup
│   ├── middleware/                  # Express middleware
│   │   ├── auth.ts                  # JWT authentication
│   │   ├── errorHandler.ts          # Global error handling
│   │   ├── notFound.ts              # 404 handler
│   │   └── validation.ts            # Request validation
│   ├── models/                      # Database models
│   │   └── Image.ts                  # Image entity model
│   ├── routes/                      # API route handlers
│   │   ├── admin.ts                 # Admin-specific routes
│   │   ├── images.ts                # Image management routes
│   │   └── sync.ts                  # Sync API integration routes
│   ├── services/                    # Business logic services
│   │   ├── syncService.ts           # Sync API integration
│   │   └── uploadService.ts         # File upload handling
│   └── index.ts                     # Application entry point
├── Dockerfile                       # Docker container configuration
├── env.example                      # Environment variables template
├── package.json                     # Node.js dependencies and scripts
└── tsconfig.json                    # TypeScript configuration
```

### Backend Architecture

**Configuration Layer (`config/`)**

- Database connection setup
- Environment-specific configurations
- Service integrations

**Middleware Layer (`middleware/`)**

- Authentication and authorization
- Request validation and sanitization
- Error handling and logging
- CORS and security headers

**Data Layer (`models/`)**

- Sequelize ORM models
- Database schema definitions
- Data validation rules

**API Layer (`routes/`)**

- RESTful API endpoints
- Route-specific business logic
- Request/response handling

**Service Layer (`services/`)**

- External API integrations
- File processing and storage
- Complex business logic

**Key Technologies**

- **Express.js**: Web application framework
- **Sequelize**: PostgreSQL ORM
- **JWT**: Authentication tokens
- **AWS S3**: Cloud storage integration
- **Sync API**: Lip-sync video generation

## 🎛️ Admin Dashboard Structure (`admin-dashboard/`)

```
admin-dashboard/
├── public/
│   └── index.html                   # HTML template
├── src/
│   ├── components/                  # Reusable React components
│   │   ├── DialogueDialog.tsx       # Dialogue management dialog
│   │   ├── ImageUploadDialog.tsx    # Image upload dialog
│   │   └── Layout.tsx               # Main layout component
│   ├── pages/                       # Page components
│   │   ├── Analytics.tsx            # Analytics dashboard
│   │   ├── Dashboard.tsx            # Main dashboard
│   │   ├── Dialogues.tsx            # Dialogue management
│   │   ├── Images.tsx               # Image management
│   │   └── Settings.tsx             # Settings page
│   ├── services/                    # API integration services
│   │   ├── api.ts                   # Base API client
│   │   ├── authService.ts           # Authentication service
│   │   ├── dialogueService.ts       # Dialogue API calls
│   │   └── imageService.ts          # Image API calls
│   ├── store/                       # Redux state management
│   │   ├── slices/                  # Redux Toolkit slices
│   │   │   ├── authSlice.ts         # Authentication state
│   │   │   ├── dialogueSlice.ts     # Dialogue state
│   │   │   └── imageSlice.ts         # Image state
│   │   └── store.ts                 # Redux store configuration
│   ├── App.tsx                      # Main app component
│   ├── index.css                    # Global styles
│   └── index.tsx                    # Application entry point
├── Dockerfile                       # Docker container configuration
└── package.json                     # React dependencies and scripts
```

### Admin Dashboard Architecture

**Component Layer (`components/`)**

- Reusable UI components
- Dialog and modal components
- Layout and navigation components

**Page Layer (`pages/`)**

- Main application pages
- Route-specific components
- Page-level state management

**Service Layer (`services/`)**

- API integration layer
- HTTP client configuration
- Service-specific business logic

**State Management (`store/`)**

- Redux Toolkit slices
- Global application state
- Async action handling

**Key Technologies**

- **React 18**: Modern React with hooks
- **TypeScript**: Type-safe development
- **Material-UI**: Component library
- **Redux Toolkit**: State management
- **React Router**: Client-side routing

## 📚 Documentation Structure (`docs/`)

```
docs/
├── API.md                          # API documentation
├── ARCHITECTURE.md                 # This file - architecture overview
└── SETUP.md                        # Development setup guide
```

## 🐳 Containerization

```
docker-compose.yml                  # Multi-service orchestration
├── Backend service configuration
├── Admin dashboard service
├── Database service (PostgreSQL)
└── Network configuration
```

## 🔧 Development Workflow

### Mobile App Development

1. **Android Studio**: Primary IDE for Kotlin development
2. **ARCore Testing**: Physical device required for AR testing
3. **Gradle Build System**: Dependency management and compilation
4. **Jetpack Compose**: Hot reload for UI development

### Backend Development

1. **Node.js Runtime**: JavaScript/TypeScript execution
2. **TypeScript Compiler**: Type checking and compilation
3. **Express.js**: Web server and API development
4. **Database Migrations**: Schema management

### Admin Dashboard Development

1. **React Development Server**: Hot reload and development tools
2. **TypeScript**: Type-safe React development
3. **Material-UI**: Component-based UI development
4. **Redux DevTools**: State management debugging

## 🚀 Deployment Architecture

### Production Environment

- **Mobile App**: Google Play Store distribution
- **Backend**: Containerized deployment (AWS/DigitalOcean)
- **Admin Dashboard**: Static hosting (Netlify/Vercel)
- **Database**: Managed PostgreSQL service
- **Storage**: AWS S3 for media assets

### Development Environment

- **Local Development**: Docker Compose for services
- **Database**: Local PostgreSQL or Docker container
- **Storage**: Local file system or S3 development bucket
- **Mobile Testing**: Android Studio emulator or physical device

## 📊 Data Flow

### Image Recognition Flow

1. **Mobile App**: Captures image with ARCore
2. **Backend**: Processes image and matches against database
3. **Sync API**: Generates lip-synced video
4. **Mobile App**: Displays AR overlay with talking head

### Content Management Flow

1. **Admin Dashboard**: Upload images and dialogues
2. **Backend**: Store in database and cloud storage
3. **Mobile App**: Fetch content via API
4. **Analytics**: Track usage and performance

## 🔒 Security Considerations

### Authentication

- **JWT Tokens**: Secure API access
- **Role-based Access**: Admin vs user permissions
- **Token Expiration**: Automatic session management

### Data Protection

- **Input Validation**: Sanitize all user inputs
- **File Upload Security**: Validate file types and sizes
- **CORS Configuration**: Restrict cross-origin requests
- **Environment Variables**: Secure configuration management

## 📈 Scalability Considerations

### Backend Scalability

- **Database Indexing**: Optimize query performance
- **Caching Strategy**: Redis for frequently accessed data
- **Load Balancing**: Multiple server instances
- **CDN Integration**: Fast content delivery

### Mobile App Scalability

- **Offline Support**: Local caching for offline usage
- **Image Optimization**: Compress and resize images
- **Memory Management**: Efficient AR rendering
- **Battery Optimization**: Minimize background processing

This architecture provides a solid foundation for the TalkAR application, ensuring scalability, maintainability, and optimal performance across all platforms.

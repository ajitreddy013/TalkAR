# TalkAR Deployment Guide

This guide covers the complete deployment process for the TalkAR application, including development, staging, and production environments.

## 🏗️ Architecture Overview

### Production Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Load Balancer │    │   CDN (CloudFlare) │    │   SSL/TLS      │
│   (nginx)       │    │                 │    │   (Let's Encrypt)│
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
┌─────────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│  │   Frontend  │  │   Backend   │  │  Database   │           │
│  │   (React)   │  │  (Node.js)  │  │ (PostgreSQL)│           │
│  └─────────────┘  └─────────────┘  └─────────────┘           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│  │ Monitoring  │  │   Logging   │  │   Storage   │           │
│  │(Prometheus) │  │  (ELK Stack)│  │   (AWS S3)  │           │
│  └─────────────┘  └─────────────┘  └─────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

## 🚀 Deployment Environments

### 1. Development Environment

#### Prerequisites

- Docker and Docker Compose
- Node.js 18+
- Android Studio (for mobile development)

#### Quick Start

```bash
# Clone repository
git clone https://github.com/ajitreddy013/TalkAR.git
cd TalkAR

# Start all services
docker-compose up -d

# Access services
# Backend API: http://localhost:3000
# Admin Dashboard: http://localhost:3001
# Database: localhost:5432
```

#### Development Setup

```bash
# Backend development
cd backend
npm install
npm run dev

# Frontend development
cd admin-dashboard
npm install
npm start

# Mobile app development
# Open mobile-app/ in Android Studio
```

### 2. Staging Environment

#### Prerequisites

- Kubernetes cluster
- kubectl configured
- Docker registry access
- SSL certificates

#### Deployment Steps

```bash
# Build and push images
docker build -t your-registry/talkar-backend:staging ./backend
docker build -t your-registry/talkar-frontend:staging ./admin-dashboard
docker push your-registry/talkar-backend:staging
docker push your-registry/talkar-frontend:staging

# Deploy to staging
kubectl apply -f k8s/ -n talkar-staging

# Verify deployment
kubectl get pods -n talkar-staging
kubectl get services -n talkar-staging
```

#### Staging Configuration

```yaml
# staging-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: talkar-staging-config
data:
  NODE_ENV: "staging"
  DATABASE_URL: "postgresql://user:pass@staging-db:5432/talkar_staging"
  CORS_ORIGIN: "https://staging-admin.talkar.com"
```

### 3. Production Environment

#### Prerequisites

- Production Kubernetes cluster
- SSL certificates (Let's Encrypt)
- Monitoring stack (Prometheus, Grafana)
- Logging stack (ELK)
- Backup strategy

#### Production Deployment

```bash
# 1. Create production namespace
kubectl create namespace talkar-production

# 2. Create secrets
kubectl apply -f k8s/secrets.yaml -n talkar-production

# 3. Deploy database
kubectl apply -f k8s/database-deployment.yaml -n talkar-production

# 4. Deploy backend
kubectl apply -f k8s/backend-deployment.yaml -n talkar-production

# 5. Deploy frontend
kubectl apply -f k8s/frontend-deployment.yaml -n talkar-production

# 6. Deploy monitoring
kubectl apply -f monitoring/ -n talkar-production

# 7. Verify deployment
kubectl get all -n talkar-production
```

## 🔧 Configuration Management

### Environment Variables

#### Backend Configuration

```bash
# Database
DATABASE_URL=postgresql://user:password@host:5432/database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=talkar_production
DB_USER=talkar_user
DB_PASSWORD=secure_password

# Server
PORT=3000
NODE_ENV=production

# JWT
JWT_SECRET=your-super-secret-jwt-key
JWT_EXPIRES_IN=7d

# AWS S3
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
AWS_REGION=us-east-1
AWS_S3_BUCKET=talkar-production-assets

# Sync API
SYNC_API_URL=https://api.sync.com/v1
SYNC_API_KEY=your-sync-api-key

# CORS
CORS_ORIGIN=https://admin.talkar.com
```

#### Frontend Configuration

```bash
# API
REACT_APP_API_URL=https://api.talkar.com/v1

# Firebase
REACT_APP_FIREBASE_API_KEY=your-firebase-api-key
REACT_APP_FIREBASE_AUTH_DOMAIN=talkar-app.firebaseapp.com
REACT_APP_FIREBASE_PROJECT_ID=talkar-app
REACT_APP_FIREBASE_STORAGE_BUCKET=talkar-app.appspot.com
REACT_APP_FIREBASE_MESSAGING_SENDER_ID=your-sender-id
REACT_APP_FIREBASE_APP_ID=your-app-id
```

### Secrets Management

#### Kubernetes Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: talkar-secrets
type: Opaque
data:
  database-url: <base64-encoded-database-url>
  jwt-secret: <base64-encoded-jwt-secret>
  postgres-password: <base64-encoded-postgres-password>
  aws-access-key-id: <base64-encoded-aws-access-key>
  aws-secret-access-key: <base64-encoded-aws-secret-key>
  sync-api-key: <base64-encoded-sync-api-key>
  firebase-api-key: <base64-encoded-firebase-api-key>
  firebase-sender-id: <base64-encoded-firebase-sender-id>
  firebase-app-id: <base64-encoded-firebase-app-id>
```

## 📊 Monitoring and Observability

### Prometheus Configuration

```yaml
# prometheus-config.yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: "talkar-backend"
    static_configs:
      - targets: ["talkar-backend-service:3000"]
    metrics_path: "/metrics"
    scrape_interval: 10s

  - job_name: "talkar-frontend"
    static_configs:
      - targets: ["talkar-frontend-service:3000"]
    metrics_path: "/metrics"
    scrape_interval: 10s
```

### Grafana Dashboards

- **Application Metrics**: Response time, error rate, throughput
- **Infrastructure Metrics**: CPU, memory, disk usage
- **Database Metrics**: Connection pool, query performance
- **Business Metrics**: User registrations, image uploads, sync requests

### Logging Configuration

```yaml
# fluentd-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluentd-config
data:
  fluent.conf: |
    <source>
      @type tail
      path /var/log/containers/*.log
      pos_file /var/log/fluentd-containers.log.pos
      tag kubernetes.*
      format json
    </source>

    <match kubernetes.**>
      @type elasticsearch
      host elasticsearch.logging.svc.cluster.local
      port 9200
      index_name talkar-logs
    </match>
```

## 🔒 Security Configuration

### SSL/TLS Setup

```yaml
# ssl-certificate.yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: talkar-ssl-cert
spec:
  secretName: talkar-ssl-secret
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
    - api.talkar.com
    - admin.talkar.com
```

### Network Policies

```yaml
# network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: talkar-network-policy
spec:
  podSelector:
    matchLabels:
      app: talkar-backend
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: talkar-frontend
      ports:
        - protocol: TCP
          port: 3000
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: talkar-postgres
      ports:
        - protocol: TCP
          port: 5432
```

## 🚀 CI/CD Pipeline

### GitHub Actions Workflow

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build and push images
        run: |
          docker build -t ${{ secrets.REGISTRY }}/talkar-backend:${{ github.sha }} ./backend
          docker build -t ${{ secrets.REGISTRY }}/talkar-frontend:${{ github.sha }} ./admin-dashboard
          docker push ${{ secrets.REGISTRY }}/talkar-backend:${{ github.sha }}
          docker push ${{ secrets.REGISTRY }}/talkar-frontend:${{ github.sha }}

      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/talkar-backend backend=${{ secrets.REGISTRY }}/talkar-backend:${{ github.sha }} -n talkar-production
          kubectl set image deployment/talkar-frontend frontend=${{ secrets.REGISTRY }}/talkar-frontend:${{ github.sha }} -n talkar-production
          kubectl rollout status deployment/talkar-backend -n talkar-production
          kubectl rollout status deployment/talkar-frontend -n talkar-production
```

## 📱 Mobile App Deployment

### Android App Store Deployment

```bash
# 1. Build release APK
cd mobile-app
./gradlew assembleRelease

# 2. Sign APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore talkar-release-key.keystore app-release-unsigned.apk talkar

# 3. Align APK
zipalign -v 4 app-release-unsigned.apk talkar-release.apk

# 4. Upload to Google Play Console
# - Go to Google Play Console
# - Create new release
# - Upload APK
# - Fill release notes
# - Publish to production
```

### iOS App Store Deployment (Future)

```bash
# 1. Build for iOS
cd mobile-app-ios
xcodebuild -workspace TalkAR.xcworkspace -scheme TalkAR -configuration Release -archivePath TalkAR.xcarchive archive

# 2. Export for App Store
xcodebuild -exportArchive -archivePath TalkAR.xcarchive -exportPath . -exportOptionsPlist ExportOptions.plist

# 3. Upload to App Store Connect
# - Use Xcode or Application Loader
# - Submit for review
```

## 🔄 Backup and Recovery

### Database Backup

```bash
# Daily backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump $DATABASE_URL > backup_$DATE.sql
aws s3 cp backup_$DATE.sql s3://talkar-backups/database/
```

### Application Backup

```bash
# Backup application data
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
kubectl get all -n talkar-production -o yaml > app_backup_$DATE.yaml
aws s3 cp app_backup_$DATE.yaml s3://talkar-backups/kubernetes/
```

## 🚨 Disaster Recovery

### Recovery Procedures

1. **Database Recovery**

   ```bash
   # Restore from backup
   psql $DATABASE_URL < backup_20240101_120000.sql
   ```

2. **Application Recovery**

   ```bash
   # Restore Kubernetes resources
   kubectl apply -f app_backup_20240101_120000.yaml
   ```

3. **Full System Recovery**
   ```bash
   # 1. Restore database
   # 2. Deploy application
   # 3. Restore secrets
   # 4. Verify functionality
   ```

## 📈 Performance Optimization

### Backend Optimization

- **Database Indexing**: Optimize query performance
- **Connection Pooling**: Manage database connections
- **Caching**: Redis for frequently accessed data
- **CDN**: CloudFlare for static assets

### Frontend Optimization

- **Code Splitting**: Lazy load components
- **Image Optimization**: WebP format, compression
- **Bundle Analysis**: Webpack bundle analyzer
- **Service Workers**: Offline functionality

### Mobile App Optimization

- **Proguard**: Code obfuscation and optimization
- **Image Compression**: Reduce APK size
- **AR Performance**: Optimize ARCore usage
- **Battery Optimization**: Efficient background processing

## 🔍 Troubleshooting

### Common Issues

1. **Database Connection Issues**

   ```bash
   kubectl logs -f deployment/talkar-backend -n talkar-production
   kubectl describe pod <pod-name> -n talkar-production
   ```

2. **Image Upload Issues**

   ```bash
   # Check S3 credentials
   aws s3 ls s3://talkar-production-assets

   # Check file permissions
   kubectl exec -it <pod-name> -n talkar-production -- ls -la /tmp
   ```

3. **Sync API Issues**
   ```bash
   # Check API connectivity
   curl -H "Authorization: Bearer $SYNC_API_KEY" https://api.sync.com/v1/voices
   ```

### Monitoring Commands

```bash
# Check pod status
kubectl get pods -n talkar-production

# Check service endpoints
kubectl get endpoints -n talkar-production

# Check ingress
kubectl get ingress -n talkar-production

# Check logs
kubectl logs -f deployment/talkar-backend -n talkar-production
```

This deployment guide provides comprehensive instructions for deploying the TalkAR application across all environments with proper monitoring, security, and backup strategies.

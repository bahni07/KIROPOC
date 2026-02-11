# Deployment Guide

This guide covers deploying the User Registration Service to various platforms.

## Prerequisites

- GitHub repository with the code
- Docker Hub account (for Docker deployments)
- Target platform account (Railway, Render, AWS, etc.)

---

## Option 1: Railway (Recommended - Easiest & Free Tier)

### Why Railway?
- ✅ Free tier: $5 credit/month
- ✅ Automatic deployments from GitHub
- ✅ Built-in PostgreSQL
- ✅ Zero configuration needed
- ✅ Automatic HTTPS

### Steps:

1. **Sign up at Railway**
   - Go to [railway.app](https://railway.app)
   - Sign in with GitHub

2. **Create New Project**
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Choose your repository

3. **Add PostgreSQL Database**
   - Click "New" → "Database" → "Add PostgreSQL"
   - Railway will automatically create and link the database

4. **Configure Environment Variables**
   ```
   EMAIL_USERNAME=your_email@gmail.com
   EMAIL_PASSWORD=your_app_password
   GOOGLE_CLIENT_ID=your_google_client_id
   GOOGLE_CLIENT_SECRET=your_google_client_secret
   AMAZON_CLIENT_ID=your_amazon_client_id
   AMAZON_CLIENT_SECRET=your_amazon_client_secret
   ENCRYPTION_KEY=your_base64_encryption_key
   ```

5. **Deploy**
   - Railway automatically deploys on every push to main
   - Get your public URL from the Railway dashboard

6. **Setup GitHub Actions Webhook** (Optional)
   - In Railway: Settings → Webhooks → Copy webhook URL
   - In GitHub: Settings → Secrets → Add `RAILWAY_WEBHOOK_URL`

---

## Option 2: Render

### Why Render?
- ✅ Free tier available
- ✅ Automatic deployments
- ✅ Managed PostgreSQL
- ✅ Easy to use

### Steps:

1. **Sign up at Render**
   - Go to [render.com](https://render.com)
   - Sign in with GitHub

2. **Create Web Service**
   - Click "New" → "Web Service"
   - Connect your GitHub repository
   - Select branch: `main`

3. **Configure Build Settings**
   ```
   Build Command: mvn clean package -DskipTests
   Start Command: java -jar target/user-registration-1.0.0.jar
   ```

4. **Add PostgreSQL Database**
   - Click "New" → "PostgreSQL"
   - Copy the Internal Database URL

5. **Set Environment Variables**
   ```
   SPRING_DATASOURCE_URL=<internal_database_url>
   EMAIL_USERNAME=your_email@gmail.com
   EMAIL_PASSWORD=your_app_password
   GOOGLE_CLIENT_ID=your_google_client_id
   GOOGLE_CLIENT_SECRET=your_google_client_secret
   AMAZON_CLIENT_ID=your_amazon_client_id
   AMAZON_CLIENT_SECRET=your_amazon_client_secret
   ENCRYPTION_KEY=your_base64_encryption_key
   ```

6. **Deploy**
   - Click "Create Web Service"
   - Render will build and deploy automatically

---

## Option 3: Docker + Any Platform

### Build and Push Docker Image

```bash
# Build image
docker build -t your-username/user-registration:latest .

# Login to Docker Hub
docker login

# Push image
docker push your-username/user-registration:latest
```

### Deploy to Various Platforms

#### AWS ECS (Elastic Container Service)

1. Create ECS cluster
2. Create task definition using your Docker image
3. Create service with load balancer
4. Set environment variables in task definition

#### Google Cloud Run

```bash
# Build and push to Google Container Registry
gcloud builds submit --tag gcr.io/PROJECT_ID/user-registration

# Deploy to Cloud Run
gcloud run deploy user-registration \
  --image gcr.io/PROJECT_ID/user-registration \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="SPRING_DATASOURCE_URL=...,EMAIL_USERNAME=..."
```

#### Azure Container Instances

```bash
# Create resource group
az group create --name user-registration-rg --location eastus

# Deploy container
az container create \
  --resource-group user-registration-rg \
  --name user-registration \
  --image your-username/user-registration:latest \
  --dns-name-label user-registration \
  --ports 8080 \
  --environment-variables \
    SPRING_DATASOURCE_URL=... \
    EMAIL_USERNAME=...
```

---

## Option 4: Kubernetes

### Create Kubernetes Manifests

**deployment.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-registration
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-registration
  template:
    metadata:
      labels:
        app: user-registration
    spec:
      containers:
      - name: user-registration
        image: your-username/user-registration:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database-url
        - name: EMAIL_USERNAME
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: email-username
        # Add other environment variables
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
```

**service.yaml**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-registration
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: user-registration
```

**Deploy:**
```bash
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

---

## Database Migration

### Using Neon (Recommended)

1. Create Neon project at [neon.tech](https://neon.tech)
2. Copy connection string
3. Set as `SPRING_DATASOURCE_URL` environment variable
4. Flyway will automatically run migrations on startup

### Using Managed PostgreSQL

Most platforms offer managed PostgreSQL:
- **Railway**: Built-in PostgreSQL
- **Render**: Managed PostgreSQL
- **AWS**: RDS PostgreSQL
- **GCP**: Cloud SQL
- **Azure**: Azure Database for PostgreSQL

---

## Environment Variables Reference

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection string | Yes | `jdbc:postgresql://host:5432/db` |
| `SPRING_DATASOURCE_USERNAME` | Database username | Yes | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | Yes | `password123` |
| `EMAIL_USERNAME` | SMTP email username | Yes | `noreply@example.com` |
| `EMAIL_PASSWORD` | SMTP email password | Yes | `app_password` |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | Yes | `123456.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Google OAuth secret | Yes | `GOCSPX-...` |
| `AMAZON_CLIENT_ID` | Amazon OAuth client ID | Yes | `amzn1.application...` |
| `AMAZON_CLIENT_SECRET` | Amazon OAuth secret | Yes | `amzn1.oa2-cs...` |
| `ENCRYPTION_KEY` | 32-byte base64 key for AES-256 | Yes | `dGhpc2lzYTMyYnl0ZWVuY3J5cHRpb25rZXk=` |

---

## Monitoring and Logging

### Health Checks

The application exposes health check endpoints:

```bash
# Basic health check
curl https://your-app.com/actuator/health

# Detailed health (requires authentication)
curl https://your-app.com/actuator/health -H "Authorization: Bearer <token>"
```

### Application Logs

Most platforms provide built-in logging:
- **Railway**: View logs in dashboard
- **Render**: View logs in dashboard
- **AWS**: CloudWatch Logs
- **GCP**: Cloud Logging
- **Azure**: Application Insights

### Custom Monitoring

Add monitoring tools:
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **Sentry**: Error tracking
- **New Relic**: APM

---

## Scaling

### Horizontal Scaling

Most platforms support auto-scaling:

**Railway:**
```bash
# Scale to 3 replicas
railway scale --replicas 3
```

**Kubernetes:**
```bash
# Scale deployment
kubectl scale deployment user-registration --replicas=3

# Auto-scaling
kubectl autoscale deployment user-registration --min=2 --max=10 --cpu-percent=80
```

### Vertical Scaling

Increase resources per instance:
- **Railway**: Upgrade plan for more CPU/RAM
- **Render**: Select larger instance type
- **Kubernetes**: Update resource requests/limits

---

## SSL/TLS

Most platforms provide automatic HTTPS:
- ✅ Railway: Automatic
- ✅ Render: Automatic
- ✅ Google Cloud Run: Automatic
- ⚠️ AWS/Azure: Configure load balancer with certificate

---

## Troubleshooting

### Application Won't Start

1. Check logs for errors
2. Verify environment variables are set
3. Ensure database is accessible
4. Check Java version (must be 17+)

### Database Connection Issues

1. Verify connection string format
2. Check database credentials
3. Ensure database is running
4. Check network/firewall rules

### OAuth Not Working

1. Verify OAuth credentials
2. Check redirect URIs match
3. Ensure HTTPS is enabled
4. Verify OAuth provider settings

---

## Cost Estimates

### Free Tier Options

| Platform | Free Tier | Limits |
|----------|-----------|--------|
| **Railway** | $5 credit/month | ~500 hours compute |
| **Render** | Free plan | 750 hours/month |
| **Neon** | Free forever | 512 MB storage |
| **Vercel** | Free | Serverless functions |

### Paid Options (Monthly)

| Platform | Starter | Production |
|----------|---------|------------|
| **Railway** | $5/month | $20-50/month |
| **Render** | $7/month | $25-85/month |
| **AWS** | $15-30/month | $100-500/month |
| **GCP** | $10-25/month | $80-400/month |

---

## Next Steps

1. Choose deployment platform
2. Set up GitHub repository
3. Configure environment variables
4. Push to main branch
5. Monitor deployment
6. Set up custom domain (optional)
7. Configure monitoring and alerts

For questions or issues, refer to the main README.md or open an issue on GitHub.

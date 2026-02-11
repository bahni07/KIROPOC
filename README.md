# User Registration Service

A production-ready Spring Boot application for user registration with email/password and OAuth2 (Google & Amazon) support.

## Features

- ✅ Email/Password Registration with verification
- ✅ OAuth2 Integration (Google & Amazon)
- ✅ Email verification with secure tokens
- ✅ Duplicate email prevention
- ✅ Password hashing with BCrypt
- ✅ Security features (CSRF, CORS, HTTPS)
- ✅ Dual database support (H2 for local, PostgreSQL for production)
- ✅ CI/CD pipeline with GitHub Actions
- ✅ Docker containerization

## Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Build Tool**: Maven 3.9+
- **Database**: PostgreSQL (Neon) / H2 (local)
- **Security**: Spring Security with OAuth2
- **Testing**: JUnit 5, jqwik (property-based testing)

## Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- Docker (optional, for containerization)
- PostgreSQL database (or use H2 for local development)

## Quick Start

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd user-registration
```

### 2. Configure environment variables

Create a `.env` file in the project root:

```env
# Database (use Neon or local PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:postgresql://your-neon-host/neondb
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# Email Configuration
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password

# OAuth2 - Google
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# OAuth2 - Amazon
AMAZON_CLIENT_ID=your_amazon_client_id
AMAZON_CLIENT_SECRET=your_amazon_client_secret

# Encryption Key (32 bytes base64-encoded)
ENCRYPTION_KEY=your_base64_encryption_key
```

### 3. Run with H2 (Local Development)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

The application will start on `http://localhost:8080` with an in-memory H2 database.

### 4. Run with PostgreSQL (Production)

```bash
mvn spring-boot:run
```

## API Endpoints

### Registration

**Register with Email**
```http
POST /api/v1/register/email
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Verify Email**
```http
GET /api/v1/register/verify?token=<verification_token>
```

**Initiate OAuth Registration**
```http
POST /api/v1/register/oauth/initiate
Content-Type: application/json

{
  "provider": "google",
  "redirectUri": "http://localhost:3000/callback"
}
```

**OAuth Callback**
```http
GET /api/v1/register/oauth/callback/{provider}?code=<auth_code>&state=<state>
```

### Health Check

```http
GET /actuator/health
```

## Running Tests

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### All Tests with Coverage
```bash
mvn clean verify jacoco:report
```

View coverage report at `target/site/jacoco/index.html`

## Docker

### Build Docker Image

```bash
docker build -t user-registration:latest .
```

### Run Docker Container

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host/db \
  -e SPRING_DATASOURCE_USERNAME=user \
  -e SPRING_DATASOURCE_PASSWORD=pass \
  -e EMAIL_USERNAME=email@example.com \
  -e EMAIL_PASSWORD=password \
  -e GOOGLE_CLIENT_ID=google_id \
  -e GOOGLE_CLIENT_SECRET=google_secret \
  -e AMAZON_CLIENT_ID=amazon_id \
  -e AMAZON_CLIENT_SECRET=amazon_secret \
  -e ENCRYPTION_KEY=your_key \
  user-registration:latest
```

## CI/CD Pipeline

The project includes a GitHub Actions workflow that:

1. ✅ Builds the project on every push/PR
2. ✅ Runs unit and integration tests
3. ✅ Generates test coverage reports
4. ✅ Builds Docker image (on releases)
5. ✅ Deploys to production (on releases)

### Automated Setup (Recommended)

Run the automated setup script to configure everything:

**Windows (PowerShell):**
```powershell
.\scripts\setup-pipeline.ps1
```

**Linux/Mac (Bash):**
```bash
chmod +x scripts/setup-pipeline.sh
./scripts/setup-pipeline.sh
```

The script will:
- Configure GitHub secrets (Docker Hub, deployment webhooks)
- Set up branch protection rules
- Create `develop` branch
- Configure CI/CD triggers

See [scripts/README.md](scripts/README.md) for detailed instructions.

### Manual Setup

If you prefer manual setup:

1. Install [GitHub CLI](https://cli.github.com/)
2. Add secrets to your GitHub repository:
   - `DOCKER_USERNAME`: Your Docker Hub username
   - `DOCKER_PASSWORD`: Your Docker Hub password/token
   - `RAILWAY_WEBHOOK_URL`: Railway deployment webhook (or other platform)
3. Configure branch protection rules (see [BRANCHING_STRATEGY.md](BRANCHING_STRATEGY.md))
4. Create `develop` branch: `git checkout -b develop && git push -u origin develop`

### Deployment Workflow

1. Push to `develop` → Tests run automatically
2. Create PR to `main` → Tests run, requires approval
3. Create release → Builds Docker image and deploys

See [BRANCHING_STRATEGY.md](BRANCHING_STRATEGY.md) for complete workflow details.

## Deployment Options

### Option 1: Railway (Recommended - Free Tier)

1. Sign up at [railway.app](https://railway.app)
2. Create new project from GitHub repo
3. Add environment variables
4. Deploy automatically on push

### Option 2: Render

1. Sign up at [render.com](https://render.com)
2. Create new Web Service from GitHub repo
3. Add environment variables
4. Deploy automatically on push

### Option 3: AWS/GCP/Azure

Use the Docker image with any container orchestration platform (ECS, Cloud Run, AKS, etc.)

## Database Setup

### Using Neon (Recommended)

1. Sign up at [neon.tech](https://neon.tech)
2. Create a new project
3. Copy the connection string
4. Update `SPRING_DATASOURCE_URL` in your environment

### Using Local PostgreSQL

```bash
# Create database
createdb user_registration

# Run migrations (Flyway will handle this automatically)
mvn flyway:migrate
```

## Security Considerations

- ✅ Passwords hashed with BCrypt (cost factor 12)
- ✅ Email verification tokens hashed with SHA-256
- ✅ OAuth tokens encrypted with AES-256-GCM
- ✅ CSRF protection enabled
- ✅ CORS configured
- ✅ HTTPS/TLS enforced in production
- ✅ Security headers (HSTS, X-Frame-Options, CSP)
- ✅ Sensitive data sanitized in logs

## Project Structure

```
src/
├── main/
│   ├── java/com/example/userregistration/
│   │   ├── config/          # Security, OAuth2 configuration
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data transfer objects
│   │   ├── exception/       # Exception handlers
│   │   ├── filter/          # Security filters
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Data access layer
│   │   └── service/         # Business logic
│   └── resources/
│       ├── application.properties
│       ├── application-test.properties
│       └── db/migration/    # Flyway migrations
└── test/                    # Unit and integration tests
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please open an issue on GitHub.

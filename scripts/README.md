# Pipeline Setup Scripts

Automated scripts to configure your CI/CD pipeline with minimal manual steps.

## What Gets Automated

These scripts automatically configure:

1. **GitHub Secrets** - Docker Hub credentials, deployment webhooks
2. **Branch Protection Rules** - Require PR reviews, status checks
3. **Branch Creation** - Creates `develop` branch if needed
4. **Repository Settings** - Configures CI/CD triggers

## Prerequisites

1. **GitHub CLI** - Install from [cli.github.com](https://cli.github.com/)
   
   Windows (PowerShell):
   ```powershell
   winget install --id GitHub.cli
   ```
   
   Or download installer from the website.

2. **Git** - Already installed (you're using it)

3. **GitHub Repository** - Already created at https://github.com/bahni07/KIROPOC

## Usage

### Windows (PowerShell)

```powershell
# Run the setup script
.\scripts\setup-pipeline.ps1
```

### Linux/Mac (Bash)

```bash
# Make script executable
chmod +x scripts/setup-pipeline.sh

# Run the setup script
./scripts/setup-pipeline.sh
```

## What the Script Does

### Step 1: Configure GitHub Secrets

The script will prompt you for:

1. **Docker Hub Username** - Your Docker Hub account username
2. **Docker Hub Password** - Your Docker Hub password or access token
   - Get token at: https://hub.docker.com/settings/security
3. **Deployment Platform** - Choose Railway, Render, or skip
4. **Webhook URL** - Deployment webhook for automatic deployments

### Step 2: Branch Protection Rules

Automatically configures:

- **main branch**:
  - Requires 1 PR approval
  - Requires tests to pass
  - No direct commits allowed
  
- **develop branch**:
  - Requires tests to pass
  - No approval required (for faster iteration)

### Step 3: Create develop Branch

If `develop` branch doesn't exist, creates it and pushes to GitHub.

## After Running the Script

Your pipeline is ready! Here's the workflow:

### 1. Create Feature Branch

```bash
git checkout develop
git checkout -b feature/my-feature
```

### 2. Make Changes

```bash
# Edit files
git add .
git commit -m "Add my feature"
git push origin feature/my-feature
```

### 3. Create Pull Request

```bash
# Using GitHub CLI
gh pr create --base develop --title "Add my feature"

# Or go to GitHub web interface
```

### 4. Merge to develop

After tests pass and review is complete, merge the PR.

### 5. Deploy to Production

When ready to deploy:

```bash
# Create PR from develop to main
gh pr create --base main --head develop --title "Release v1.0.0"

# After merging, create a release
gh release create v1.0.0 --title "Release v1.0.0" --notes "Initial release"
```

The release triggers automatic deployment!

## Troubleshooting

### "gh: command not found"

Install GitHub CLI:
- Windows: `winget install --id GitHub.cli`
- Mac: `brew install gh`
- Linux: See [cli.github.com](https://cli.github.com/)

### "Not authenticated with GitHub"

Run:
```bash
gh auth login
```

Follow the prompts to authenticate.

### "Could not set branch protection"

You need admin access to the repository. Options:

1. Ask repository owner to run the script
2. Manually configure in GitHub:
   - Go to Settings → Branches → Add rule
   - Follow instructions in BRANCHING_STRATEGY.md

### "develop branch already exists"

That's fine! The script will skip creation and continue.

## Manual Setup (If Script Fails)

If the automated script doesn't work, follow these manual steps:

### 1. Add GitHub Secrets

Go to: https://github.com/bahni07/KIROPOC/settings/secrets/actions

Add these secrets:
- `DOCKER_USERNAME` - Your Docker Hub username
- `DOCKER_PASSWORD` - Your Docker Hub password/token
- `RAILWAY_WEBHOOK_URL` - Your deployment webhook URL

### 2. Create develop Branch

```bash
git checkout -b develop
git push -u origin develop
```

### 3. Configure Branch Protection

Go to: https://github.com/bahni07/KIROPOC/settings/branches

For `main` branch:
- ✅ Require pull request before merging
- ✅ Require approvals: 1
- ✅ Require status checks: build-and-test

For `develop` branch:
- ✅ Require pull request before merging
- ✅ Require status checks: build-and-test

## What Happens Next

Once configured:

1. **Push to develop** → Tests run automatically
2. **PR to main** → Tests run, requires approval
3. **Create release** → Builds Docker image, deploys to production

You have full control - no accidental deployments!

## Getting Help

- Check BRANCHING_STRATEGY.md for workflow details
- Check DEPLOYMENT.md for platform-specific setup
- Open an issue on GitHub if you encounter problems

## Security Notes

- Never commit secrets to git
- Use GitHub Secrets for sensitive data
- Rotate credentials regularly
- Use Docker Hub access tokens (not passwords)

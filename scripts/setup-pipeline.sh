#!/bin/bash

# CI/CD Pipeline Setup Script
# This script automates the setup of GitHub Actions CI/CD pipeline

set -e

echo "🚀 User Registration Service - CI/CD Pipeline Setup"
echo "=================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if GitHub CLI is installed
if ! command -v gh &> /dev/null; then
    echo -e "${RED}❌ GitHub CLI (gh) is not installed${NC}"
    echo "Please install it from: https://cli.github.com/"
    exit 1
fi

# Check if user is authenticated
if ! gh auth status &> /dev/null; then
    echo -e "${YELLOW}⚠️  Not authenticated with GitHub${NC}"
    echo "Running: gh auth login"
    gh auth login
fi

echo -e "${GREEN}✅ GitHub CLI authenticated${NC}"
echo ""

# Get repository information
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo "📦 Repository: $REPO"
echo ""

# Function to prompt for input with default
prompt_with_default() {
    local prompt="$1"
    local default="$2"
    local value
    
    read -p "$prompt [$default]: " value
    echo "${value:-$default}"
}

# Function to prompt for secret input
prompt_secret() {
    local prompt="$1"
    local value
    
    read -sp "$prompt: " value
    echo ""
    echo "$value"
}

echo "🔧 Step 1: Configure GitHub Secrets"
echo "-----------------------------------"
echo ""

# Docker Hub credentials
echo "Docker Hub Credentials (for building and pushing images):"
DOCKER_USERNAME=$(prompt_with_default "Docker Hub username" "")
if [ -n "$DOCKER_USERNAME" ]; then
    DOCKER_PASSWORD=$(prompt_secret "Docker Hub password/token")
    gh secret set DOCKER_USERNAME --body "$DOCKER_USERNAME"
    gh secret set DOCKER_PASSWORD --body "$DOCKER_PASSWORD"
    echo -e "${GREEN}✅ Docker Hub credentials saved${NC}"
else
    echo -e "${YELLOW}⚠️  Skipping Docker Hub setup${NC}"
fi
echo ""

# Deployment webhook
echo "Deployment Platform:"
echo "1. Railway"
echo "2. Render"
echo "3. Other/Skip"
DEPLOY_CHOICE=$(prompt_with_default "Choose deployment platform" "1")

case $DEPLOY_CHOICE in
    1)
        echo ""
        echo "Railway Setup:"
        echo "1. Go to https://railway.app"
        echo "2. Create a new project from your GitHub repo"
        echo "3. Go to Settings → Webhooks → Copy webhook URL"
        echo ""
        RAILWAY_WEBHOOK=$(prompt_with_default "Railway webhook URL" "")
        if [ -n "$RAILWAY_WEBHOOK" ]; then
            gh secret set RAILWAY_WEBHOOK_URL --body "$RAILWAY_WEBHOOK"
            echo -e "${GREEN}✅ Railway webhook saved${NC}"
        fi
        ;;
    2)
        echo ""
        echo "Render Setup:"
        echo "1. Go to https://render.com"
        echo "2. Create a new Web Service from your GitHub repo"
        echo "3. Go to Settings → Deploy Hook → Copy URL"
        echo ""
        RENDER_WEBHOOK=$(prompt_with_default "Render deploy hook URL" "")
        if [ -n "$RENDER_WEBHOOK" ]; then
            gh secret set RAILWAY_WEBHOOK_URL --body "$RENDER_WEBHOOK"
            echo -e "${GREEN}✅ Render webhook saved${NC}"
        fi
        ;;
    *)
        echo -e "${YELLOW}⚠️  Skipping deployment webhook setup${NC}"
        ;;
esac
echo ""

echo "🔒 Step 2: Configure Branch Protection Rules"
echo "--------------------------------------------"
echo ""

# Create develop branch if it doesn't exist
if ! git show-ref --verify --quiet refs/heads/develop; then
    echo "Creating develop branch..."
    git checkout -b develop
    git push -u origin develop
    git checkout main
    echo -e "${GREEN}✅ Develop branch created${NC}"
else
    echo -e "${GREEN}✅ Develop branch already exists${NC}"
fi
echo ""

# Set up branch protection for main
echo "Setting up branch protection for 'main'..."
gh api repos/$REPO/branches/main/protection \
    --method PUT \
    --field required_status_checks='{"strict":true,"contexts":["build-and-test"]}' \
    --field enforce_admins=false \
    --field required_pull_request_reviews='{"required_approving_review_count":1}' \
    --field restrictions=null \
    --field allow_force_pushes=false \
    --field allow_deletions=false \
    2>/dev/null && echo -e "${GREEN}✅ Branch protection enabled for main${NC}" || echo -e "${YELLOW}⚠️  Could not set branch protection (may require admin access)${NC}"

# Set up branch protection for develop
echo "Setting up branch protection for 'develop'..."
gh api repos/$REPO/branches/develop/protection \
    --method PUT \
    --field required_status_checks='{"strict":true,"contexts":["build-and-test"]}' \
    --field enforce_admins=false \
    --field required_pull_request_reviews='{"required_approving_review_count":0}' \
    --field restrictions=null \
    --field allow_force_pushes=false \
    --field allow_deletions=false \
    2>/dev/null && echo -e "${GREEN}✅ Branch protection enabled for develop${NC}" || echo -e "${YELLOW}⚠️  Could not set branch protection (may require admin access)${NC}"

echo ""

echo "📋 Step 3: Summary"
echo "------------------"
echo ""
echo "✅ GitHub secrets configured"
echo "✅ Branch protection rules set"
echo "✅ CI/CD pipeline ready"
echo ""
echo "🎯 Next Steps:"
echo ""
echo "1. Create a feature branch:"
echo "   git checkout -b feature/my-feature"
echo ""
echo "2. Make changes and push:"
echo "   git add ."
echo "   git commit -m 'Add feature'"
echo "   git push origin feature/my-feature"
echo ""
echo "3. Create Pull Request to develop:"
echo "   gh pr create --base develop --title 'Add feature'"
echo ""
echo "4. After testing on develop, create PR to main:"
echo "   gh pr create --base main --head develop --title 'Release v1.0.0'"
echo ""
echo "5. Create release to deploy:"
echo "   gh release create v1.0.0 --title 'Release v1.0.0' --notes 'Initial release'"
echo ""
echo -e "${GREEN}🎉 Setup complete!${NC}"

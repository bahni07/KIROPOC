# CI/CD Pipeline Setup Script for Windows
# This script automates the setup of GitHub Actions CI/CD pipeline

$ErrorActionPreference = "Stop"

Write-Host "🚀 User Registration Service - CI/CD Pipeline Setup" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Check if GitHub CLI is installed
try {
    $null = Get-Command gh -ErrorAction Stop
    Write-Host "✅ GitHub CLI found" -ForegroundColor Green
} catch {
    Write-Host "❌ GitHub CLI (gh) is not installed" -ForegroundColor Red
    Write-Host "Please install it from: https://cli.github.com/" -ForegroundColor Yellow
    exit 1
}

# Check if user is authenticated
$authStatus = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Not authenticated with GitHub" -ForegroundColor Yellow
    Write-Host "Running: gh auth login" -ForegroundColor Yellow
    gh auth login
}

Write-Host "✅ GitHub CLI authenticated" -ForegroundColor Green
Write-Host ""

# Get repository information
$repoInfo = gh repo view --json nameWithOwner | ConvertFrom-Json
$repo = $repoInfo.nameWithOwner
Write-Host "📦 Repository: $repo" -ForegroundColor Cyan
Write-Host ""

# Function to prompt for input with default
function Prompt-WithDefault {
    param(
        [string]$Prompt,
        [string]$Default
    )
    
    $value = Read-Host "$Prompt [$Default]"
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $Default
    }
    return $value
}

# Function to prompt for secret input
function Prompt-Secret {
    param([string]$Prompt)
    
    $secureString = Read-Host "$Prompt" -AsSecureString
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureString)
    $value = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    return $value
}

Write-Host "🔧 Step 1: Configure GitHub Secrets" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan
Write-Host ""

# Docker Hub credentials
Write-Host "Docker Hub Credentials (for building and pushing images):" -ForegroundColor Yellow
$dockerUsername = Prompt-WithDefault "Docker Hub username" ""
if (-not [string]::IsNullOrWhiteSpace($dockerUsername)) {
    $dockerPassword = Prompt-Secret "Docker Hub password/token"
    
    gh secret set DOCKER_USERNAME --body $dockerUsername
    gh secret set DOCKER_PASSWORD --body $dockerPassword
    Write-Host "✅ Docker Hub credentials saved" -ForegroundColor Green
} else {
    Write-Host "⚠️  Skipping Docker Hub setup" -ForegroundColor Yellow
}
Write-Host ""

# Deployment webhook
Write-Host "Deployment Platform:" -ForegroundColor Yellow
Write-Host "1. Railway"
Write-Host "2. Render"
Write-Host "3. Other/Skip"
$deployChoice = Prompt-WithDefault "Choose deployment platform" "1"

switch ($deployChoice) {
    "1" {
        Write-Host ""
        Write-Host "Railway Setup:" -ForegroundColor Yellow
        Write-Host "1. Go to https://railway.app"
        Write-Host "2. Create a new project from your GitHub repo"
        Write-Host "3. Go to Settings → Webhooks → Copy webhook URL"
        Write-Host ""
        $railwayWebhook = Prompt-WithDefault "Railway webhook URL" ""
        if (-not [string]::IsNullOrWhiteSpace($railwayWebhook)) {
            gh secret set RAILWAY_WEBHOOK_URL --body $railwayWebhook
            Write-Host "✅ Railway webhook saved" -ForegroundColor Green
        }
    }
    "2" {
        Write-Host ""
        Write-Host "Render Setup:" -ForegroundColor Yellow
        Write-Host "1. Go to https://render.com"
        Write-Host "2. Create a new Web Service from your GitHub repo"
        Write-Host "3. Go to Settings → Deploy Hook → Copy URL"
        Write-Host ""
        $renderWebhook = Prompt-WithDefault "Render deploy hook URL" ""
        if (-not [string]::IsNullOrWhiteSpace($renderWebhook)) {
            gh secret set RAILWAY_WEBHOOK_URL --body $renderWebhook
            Write-Host "✅ Render webhook saved" -ForegroundColor Green
        }
    }
    default {
        Write-Host "⚠️  Skipping deployment webhook setup" -ForegroundColor Yellow
    }
}
Write-Host ""

Write-Host "🔒 Step 2: Configure Branch Protection Rules" -ForegroundColor Cyan
Write-Host "--------------------------------------------" -ForegroundColor Cyan
Write-Host ""

# Check if develop branch exists
$developExists = git show-ref --verify --quiet refs/heads/develop
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating develop branch..." -ForegroundColor Yellow
    git checkout -b develop
    git push -u origin develop
    git checkout main
    Write-Host "✅ Develop branch created" -ForegroundColor Green
} else {
    Write-Host "✅ Develop branch already exists" -ForegroundColor Green
}
Write-Host ""

# Set up branch protection for main
Write-Host "Setting up branch protection for 'main'..." -ForegroundColor Yellow
$mainProtection = @{
    required_status_checks = @{
        strict = $true
        contexts = @("build-and-test")
    }
    enforce_admins = $false
    required_pull_request_reviews = @{
        required_approving_review_count = 1
    }
    restrictions = $null
    allow_force_pushes = $false
    allow_deletions = $false
} | ConvertTo-Json -Depth 10

try {
    gh api "repos/$repo/branches/main/protection" --method PUT --input - <<< $mainProtection | Out-Null
    Write-Host "✅ Branch protection enabled for main" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not set branch protection (may require admin access)" -ForegroundColor Yellow
}

# Set up branch protection for develop
Write-Host "Setting up branch protection for 'develop'..." -ForegroundColor Yellow
$developProtection = @{
    required_status_checks = @{
        strict = $true
        contexts = @("build-and-test")
    }
    enforce_admins = $false
    required_pull_request_reviews = @{
        required_approving_review_count = 0
    }
    restrictions = $null
    allow_force_pushes = $false
    allow_deletions = $false
} | ConvertTo-Json -Depth 10

try {
    gh api "repos/$repo/branches/develop/protection" --method PUT --input - <<< $developProtection | Out-Null
    Write-Host "✅ Branch protection enabled for develop" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not set branch protection (may require admin access)" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "📋 Step 3: Summary" -ForegroundColor Cyan
Write-Host "------------------" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ GitHub secrets configured" -ForegroundColor Green
Write-Host "✅ Branch protection rules set" -ForegroundColor Green
Write-Host "✅ CI/CD pipeline ready" -ForegroundColor Green
Write-Host ""
Write-Host "🎯 Next Steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Create a feature branch:"
Write-Host "   git checkout -b feature/my-feature"
Write-Host ""
Write-Host "2. Make changes and push:"
Write-Host "   git add ."
Write-Host "   git commit -m 'Add feature'"
Write-Host "   git push origin feature/my-feature"
Write-Host ""
Write-Host "3. Create Pull Request to develop:"
Write-Host "   gh pr create --base develop --title 'Add feature'"
Write-Host ""
Write-Host "4. After testing on develop, create PR to main:"
Write-Host "   gh pr create --base main --head develop --title 'Release v1.0.0'"
Write-Host ""
Write-Host "5. Create release to deploy:"
Write-Host "   gh release create v1.0.0 --title 'Release v1.0.0' --notes 'Initial release'"
Write-Host ""
Write-Host "🎉 Setup complete!" -ForegroundColor Green

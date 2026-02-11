# Setup GitHub Secrets for CI/CD
Write-Host "GitHub Secrets Setup for CI/CD Pipeline" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check if gh CLI is installed
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Host "GitHub CLI (gh) is not installed!" -ForegroundColor Red
    Write-Host "Install it from: https://cli.github.com/" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Or install via winget:" -ForegroundColor Yellow
    Write-Host "  winget install --id GitHub.cli" -ForegroundColor White
    exit 1
}

Write-Host "GitHub CLI found" -ForegroundColor Green
Write-Host ""

# Check if logged in
$ghStatus = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Not logged in to GitHub. Logging in..." -ForegroundColor Yellow
    gh auth login
}

Write-Host "Logged in to GitHub" -ForegroundColor Green
Write-Host ""

# Get repository info
$repo = "bahni07/KIROPOC"
Write-Host "Repository: $repo" -ForegroundColor Cyan
Write-Host ""

# Set AWS credentials
Write-Host "Setting up AWS credentials..." -ForegroundColor Yellow
Write-Host ""

$awsAccessKey = Read-Host "AWS Access Key ID"
$awsSecretKey = Read-Host "AWS Secret Access Key" -AsSecureString
$awsSecretKeyPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($awsSecretKey)
)

# Set secrets
Write-Host ""
Write-Host "Setting GitHub secrets..." -ForegroundColor Yellow

gh secret set AWS_ACCESS_KEY_ID --body $awsAccessKey --repo $repo
gh secret set AWS_SECRET_ACCESS_KEY --body $awsSecretKeyPlain --repo $repo

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ AWS_ACCESS_KEY_ID set" -ForegroundColor Green
    Write-Host "✓ AWS_SECRET_ACCESS_KEY set" -ForegroundColor Green
} else {
    Write-Host "Failed to set secrets!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "GitHub secrets configured successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Make a code change" -ForegroundColor White
Write-Host "2. Commit and push to 'develop' branch (triggers build & test)" -ForegroundColor White
Write-Host "3. Create a release on GitHub (triggers deployment)" -ForegroundColor White
Write-Host ""

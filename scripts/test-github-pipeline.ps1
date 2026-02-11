# Test GitHub Actions Pipeline
Write-Host "Testing GitHub Actions CI/CD Pipeline" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Configure GitHub secrets (if not already done)
Write-Host "Step 1: Configure GitHub Secrets" -ForegroundColor Yellow
Write-Host "--------------------------------" -ForegroundColor Yellow
$secretsConfigured = Read-Host "Have you configured AWS secrets in GitHub? (y/n)"

if ($secretsConfigured -ne "y") {
    Write-Host ""
    Write-Host "Add these secrets manually in GitHub:" -ForegroundColor Yellow
    Write-Host "  1. Go to: https://github.com/bahni07/KIROPOC/settings/secrets/actions" -ForegroundColor White
    Write-Host "  2. Click 'New repository secret'" -ForegroundColor White
    Write-Host "  3. Add: AWS_ACCESS_KEY_ID" -ForegroundColor White
    Write-Host "  4. Add: AWS_SECRET_ACCESS_KEY" -ForegroundColor White
    Write-Host ""
    exit 0
}

Write-Host "Secrets configured" -ForegroundColor Green
Write-Host ""

# Step 2: Commit and push changes
Write-Host "Step 2: Commit and Push Changes" -ForegroundColor Yellow
Write-Host "--------------------------------" -ForegroundColor Yellow

git status

Write-Host ""
$commitChanges = Read-Host "Commit and push changes to main? (y/n)"

if ($commitChanges -eq "y") {
    git add .
    git commit -m "fix: Update OAuth2 configuration and CI/CD workflow"
    
    $currentBranch = git branch --show-current
    Write-Host "Current branch: $currentBranch" -ForegroundColor Cyan
    
    if ($currentBranch -ne "main") {
        Write-Host "Switching to main branch..." -ForegroundColor Yellow
        git checkout main
    }
    
    Write-Host "Pushing to main..." -ForegroundColor Yellow
    git push origin main
    
    Write-Host ""
    Write-Host "Changes pushed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "GitHub Actions will now:" -ForegroundColor Cyan
    Write-Host "  1. Build the application" -ForegroundColor White
    Write-Host "  2. Run tests" -ForegroundColor White
    Write-Host "  3. Build Lambda JAR" -ForegroundColor White
    Write-Host "  4. Upload to S3" -ForegroundColor White
    Write-Host "  5. Update Lambda function" -ForegroundColor White
    Write-Host ""
    Write-Host "Monitor at: https://github.com/bahni07/KIROPOC/actions" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "Done!" -ForegroundColor Green

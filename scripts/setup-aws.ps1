# AWS Infrastructure Setup Script for Windows
# This script automates the deployment of User Registration Service to AWS

$ErrorActionPreference = "Stop"

Write-Host "🚀 User Registration Service - AWS Deployment Setup" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host ""

# Check if AWS CLI is installed
try {
    $null = Get-Command aws -ErrorAction Stop
    Write-Host "✅ AWS CLI found" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS CLI is not installed" -ForegroundColor Red
    Write-Host "Please install it from: https://aws.amazon.com/cli/" -ForegroundColor Yellow
    Write-Host "Or run: msiexec.exe /i https://awscli.amazonaws.com/AWSCLIV2.msi" -ForegroundColor Yellow
    exit 1
}

# Check AWS credentials
Write-Host "Checking AWS credentials..." -ForegroundColor Yellow
$awsIdentity = aws sts get-caller-identity 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ AWS credentials not configured" -ForegroundColor Red
    Write-Host "Please run: aws configure" -ForegroundColor Yellow
    exit 1
}

$identity = $awsIdentity | ConvertFrom-Json
Write-Host "✅ AWS credentials configured" -ForegroundColor Green
Write-Host "   Account: $($identity.Account)" -ForegroundColor Gray
Write-Host "   User: $($identity.Arn)" -ForegroundColor Gray
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

Write-Host "📋 Step 1: Configuration" -ForegroundColor Cyan
Write-Host "------------------------" -ForegroundColor Cyan
Write-Host ""

$stackName = Prompt-WithDefault "CloudFormation stack name" "user-registration-prod"
$environment = Prompt-WithDefault "Environment name" "production"
$region = Prompt-WithDefault "AWS region" "us-east-1"
$dbUsername = Prompt-WithDefault "Database username" "postgres"
$dbPassword = Prompt-Secret "Database password (min 8 characters)"

Write-Host ""
Write-Host "🏗️  Step 2: Deploy Infrastructure" -ForegroundColor Cyan
Write-Host "----------------------------------" -ForegroundColor Cyan
Write-Host ""

Write-Host "Deploying CloudFormation stack..." -ForegroundColor Yellow
Write-Host "This will take 10-15 minutes..." -ForegroundColor Yellow

$stackParams = @(
    "ParameterKey=EnvironmentName,ParameterValue=$environment",
    "ParameterKey=DatabaseUsername,ParameterValue=$dbUsername",
    "ParameterKey=DatabasePassword,ParameterValue=$dbPassword"
)

aws cloudformation create-stack `
    --stack-name $stackName `
    --template-body file://aws/infrastructure.yml `
    --parameters $stackParams `
    --capabilities CAPABILITY_IAM `
    --region $region

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to create stack" -ForegroundColor Red
    exit 1
}

Write-Host "⏳ Waiting for stack creation..." -ForegroundColor Yellow
aws cloudformation wait stack-create-complete `
    --stack-name $stackName `
    --region $region

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Stack creation failed" -ForegroundColor Red
    Write-Host "Check AWS Console for details: https://console.aws.amazon.com/cloudformation/" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Infrastructure deployed successfully" -ForegroundColor Green
Write-Host ""

# Get stack outputs
Write-Host "📊 Getting stack outputs..." -ForegroundColor Yellow
$outputs = aws cloudformation describe-stacks `
    --stack-name $stackName `
    --region $region `
    --query 'Stacks[0].Outputs' | ConvertFrom-Json

$albUrl = ($outputs | Where-Object { $_.OutputKey -eq "LoadBalancerURL" }).OutputValue
$ecrUri = ($outputs | Where-Object { $_.OutputKey -eq "ECRRepositoryURI" }).OutputValue
$dbEndpoint = ($outputs | Where-Object { $_.OutputKey -eq "DatabaseEndpoint" }).OutputValue
$ecsCluster = ($outputs | Where-Object { $_.OutputKey -eq "ECSClusterName" }).OutputValue
$ecsService = ($outputs | Where-Object { $_.OutputKey -eq "ECSServiceName" }).OutputValue

Write-Host "✅ Stack outputs retrieved" -ForegroundColor Green
Write-Host ""

Write-Host "🔐 Step 3: Configure Application Secrets" -ForegroundColor Cyan
Write-Host "----------------------------------------" -ForegroundColor Cyan
Write-Host ""

Write-Host "Enter your application secrets:" -ForegroundColor Yellow
$emailUsername = Prompt-WithDefault "Email username" "your_email@gmail.com"
$emailPassword = Prompt-Secret "Email password"
$googleClientId = Prompt-WithDefault "Google OAuth Client ID" "your_google_client_id"
$googleClientSecret = Prompt-Secret "Google OAuth Client Secret"
$amazonClientId = Prompt-WithDefault "Amazon OAuth Client ID" "your_amazon_client_id"
$amazonClientSecret = Prompt-Secret "Amazon OAuth Client Secret"
$encryptionKey = Prompt-WithDefault "Encryption Key (base64)" "your_base64_encryption_key"

$secretsJson = @{
    SPRING_DATASOURCE_PASSWORD = $dbPassword
    EMAIL_USERNAME = $emailUsername
    EMAIL_PASSWORD = $emailPassword
    GOOGLE_CLIENT_ID = $googleClientId
    GOOGLE_CLIENT_SECRET = $googleClientSecret
    AMAZON_CLIENT_ID = $amazonClientId
    AMAZON_CLIENT_SECRET = $amazonClientSecret
    ENCRYPTION_KEY = $encryptionKey
} | ConvertTo-Json -Compress

Write-Host "Updating secrets in AWS Secrets Manager..." -ForegroundColor Yellow
aws secretsmanager update-secret `
    --secret-id "$environment/user-registration/secrets" `
    --secret-string $secretsJson `
    --region $region

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Secrets updated successfully" -ForegroundColor Green
} else {
    Write-Host "⚠️  Failed to update secrets" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "🐳 Step 4: Build and Push Docker Image" -ForegroundColor Cyan
Write-Host "--------------------------------------" -ForegroundColor Cyan
Write-Host ""

Write-Host "Logging in to Amazon ECR..." -ForegroundColor Yellow
$ecrPassword = aws ecr get-login-password --region $region
$ecrPassword | docker login --username AWS --password-stdin $ecrUri

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to login to ECR" -ForegroundColor Red
    exit 1
}

Write-Host "Building Docker image..." -ForegroundColor Yellow
docker build -t user-registration:latest .

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to build Docker image" -ForegroundColor Red
    exit 1
}

Write-Host "Tagging and pushing image..." -ForegroundColor Yellow
docker tag user-registration:latest "$ecrUri:latest"
docker push "$ecrUri:latest"

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to push Docker image" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Docker image pushed to ECR" -ForegroundColor Green
Write-Host ""

Write-Host "🚀 Step 5: Deploy to ECS" -ForegroundColor Cyan
Write-Host "------------------------" -ForegroundColor Cyan
Write-Host ""

Write-Host "Updating ECS service..." -ForegroundColor Yellow
aws ecs update-service `
    --cluster $ecsCluster `
    --service $ecsService `
    --force-new-deployment `
    --region $region | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ ECS service updated" -ForegroundColor Green
} else {
    Write-Host "⚠️  Failed to update ECS service" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "⚙️  Step 6: Configure GitHub Secrets" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan
Write-Host ""

Write-Host "Setting up GitHub secrets for CI/CD..." -ForegroundColor Yellow
Write-Host ""
Write-Host "You need to add these secrets to GitHub:" -ForegroundColor Yellow
Write-Host "1. AWS_ACCESS_KEY_ID" -ForegroundColor White
Write-Host "2. AWS_SECRET_ACCESS_KEY" -ForegroundColor White
Write-Host ""

$setupGitHub = Read-Host "Do you want to set up GitHub secrets now? (y/n)"
if ($setupGitHub -eq "y") {
    # Check if GitHub CLI is installed
    try {
        $null = Get-Command gh -ErrorAction Stop
        
        Write-Host "Enter your AWS credentials for GitHub Actions:" -ForegroundColor Yellow
        $awsAccessKey = Read-Host "AWS Access Key ID"
        $awsSecretKey = Prompt-Secret "AWS Secret Access Key"
        
        gh secret set AWS_ACCESS_KEY_ID --body $awsAccessKey
        gh secret set AWS_SECRET_ACCESS_KEY --body $awsSecretKey
        
        Write-Host "✅ GitHub secrets configured" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  GitHub CLI not found" -ForegroundColor Yellow
        Write-Host "Install from: https://cli.github.com/" -ForegroundColor Yellow
        Write-Host "Or manually add secrets at:" -ForegroundColor Yellow
        Write-Host "https://github.com/bahni07/KIROPOC/settings/secrets/actions" -ForegroundColor White
    }
} else {
    Write-Host "⚠️  Skipping GitHub setup" -ForegroundColor Yellow
    Write-Host "Manually add secrets at:" -ForegroundColor Yellow
    Write-Host "https://github.com/bahni07/KIROPOC/settings/secrets/actions" -ForegroundColor White
}
Write-Host ""

Write-Host "📋 Step 7: Summary" -ForegroundColor Cyan
Write-Host "------------------" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Infrastructure deployed" -ForegroundColor Green
Write-Host "✅ Docker image built and pushed" -ForegroundColor Green
Write-Host "✅ Application deployed to ECS" -ForegroundColor Green
Write-Host ""
Write-Host "🌐 Application URL: $albUrl" -ForegroundColor Cyan
Write-Host "🗄️  Database Endpoint: $dbEndpoint" -ForegroundColor Cyan
Write-Host "📦 ECR Repository: $ecrUri" -ForegroundColor Cyan
Write-Host "🎯 ECS Cluster: $ecsCluster" -ForegroundColor Cyan
Write-Host "🔧 ECS Service: $ecsService" -ForegroundColor Cyan
Write-Host ""
Write-Host "🎯 Next Steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Test the application:"
Write-Host "   curl $albUrl/actuator/health"
Write-Host ""
Write-Host "2. View logs:"
Write-Host "   aws logs tail /ecs/$environment/user-registration --follow"
Write-Host ""
Write-Host "3. Create a release to trigger CI/CD:"
Write-Host "   gh release create v1.0.0 --title 'Release v1.0.0' --notes 'Initial release'"
Write-Host ""
Write-Host "4. Monitor in AWS Console:"
Write-Host "   https://console.aws.amazon.com/ecs/"
Write-Host ""
Write-Host "🎉 Setup complete!" -ForegroundColor Green

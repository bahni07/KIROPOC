# AWS Lambda Setup Script for Windows
# This script automates the deployment of User Registration Service to AWS Lambda

$ErrorActionPreference = "Stop"

Write-Host "🚀 User Registration Service - AWS Lambda Deployment" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host ""

# Check if AWS CLI is installed
try {
    $null = Get-Command aws -ErrorAction Stop
    Write-Host "✅ AWS CLI found" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS CLI is not installed" -ForegroundColor Red
    Write-Host "Please install it from: https://aws.amazon.com/cli/" -ForegroundColor Yellow
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

$stackName = Prompt-WithDefault "CloudFormation stack name" "user-registration-lambda"
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
    --template-body file://aws/lambda-infrastructure.yml `
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
    Write-Host "Check AWS Console: https://console.aws.amazon.com/cloudformation/" -ForegroundColor Yellow
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

$apiEndpoint = ($outputs | Where-Object { $_.OutputKey -eq "ApiEndpoint" }).OutputValue
$lambdaArn = ($outputs | Where-Object { $_.OutputKey -eq "LambdaFunctionArn" }).OutputValue
$deploymentBucket = ($outputs | Where-Object { $_.OutputKey -eq "DeploymentBucketName" }).OutputValue
$dbEndpoint = ($outputs | Where-Object { $_.OutputKey -eq "DatabaseEndpoint" }).OutputValue

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
    --secret-id "$environment/lambda/user-registration/secrets" `
    --secret-string $secretsJson `
    --region $region

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Secrets updated successfully" -ForegroundColor Green
} else {
    Write-Host "⚠️  Failed to update secrets" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "📦 Step 4: Build Lambda Package" -ForegroundColor Cyan
Write-Host "-------------------------------" -ForegroundColor Cyan
Write-Host ""

Write-Host "Building JAR with Maven..." -ForegroundColor Yellow
& "C:\Users\CAESAR\Downloads\apache-maven-3.9.12-bin\apache-maven-3.9.12\bin\mvn.cmd" clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to build JAR" -ForegroundColor Red
    exit 1
}

Write-Host "✅ JAR built successfully" -ForegroundColor Green
Write-Host ""

Write-Host "☁️  Step 5: Upload to S3 and Deploy" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan
Write-Host ""

Write-Host "Uploading JAR to S3..." -ForegroundColor Yellow
aws s3 cp target/user-registration-1.0.0.jar `
    "s3://$deploymentBucket/user-registration.jar" `
    --region $region

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to upload to S3" -ForegroundColor Red
    exit 1
}

Write-Host "Updating Lambda function..." -ForegroundColor Yellow
aws lambda update-function-code `
    --function-name "$environment-user-registration" `
    --s3-bucket $deploymentBucket `
    --s3-key user-registration.jar `
    --region $region | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to update Lambda function" -ForegroundColor Red
    exit 1
}

Write-Host "⏳ Waiting for Lambda update to complete..." -ForegroundColor Yellow
aws lambda wait function-updated `
    --function-name "$environment-user-registration" `
    --region $region

Write-Host "✅ Lambda function updated" -ForegroundColor Green
Write-Host ""

Write-Host "⚙️  Step 6: Configure GitHub Secrets" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan
Write-Host ""

$setupGitHub = Read-Host "Do you want to set up GitHub secrets now? (y/n)"
if ($setupGitHub -eq "y") {
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
    }
} else {
    Write-Host "⚠️  Skipping GitHub setup" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "📋 Step 7: Summary" -ForegroundColor Cyan
Write-Host "------------------" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Infrastructure deployed" -ForegroundColor Green
Write-Host "✅ Lambda function deployed" -ForegroundColor Green
Write-Host "✅ Application ready" -ForegroundColor Green
Write-Host ""
Write-Host "🌐 API Endpoint: $apiEndpoint" -ForegroundColor Cyan
Write-Host "⚡ Lambda ARN: $lambdaArn" -ForegroundColor Cyan
Write-Host "🗄️  Database: $dbEndpoint" -ForegroundColor Cyan
Write-Host "📦 S3 Bucket: $deploymentBucket" -ForegroundColor Cyan
Write-Host ""
Write-Host "🎯 Next Steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Test the API (may take 5-10 seconds for cold start):"
Write-Host "   curl $apiEndpoint/actuator/health"
Write-Host ""
Write-Host "2. Test registration:"
Write-Host "   curl -X POST $apiEndpoint/api/v1/register/email \"
Write-Host "     -H 'Content-Type: application/json' \"
Write-Host "     -d '{\"email\":\"test@example.com\",\"password\":\"Test123!\",\"firstName\":\"John\",\"lastName\":\"Doe\"}'"
Write-Host ""
Write-Host "3. View Lambda logs:"
Write-Host "   aws logs tail /aws/lambda/$environment-user-registration --follow"
Write-Host ""
Write-Host "4. Monitor in AWS Console:"
Write-Host "   https://console.aws.amazon.com/lambda/"
Write-Host ""
Write-Host "⚠️  Note: First request will have 2-5 second cold start delay" -ForegroundColor Yellow
Write-Host ""
Write-Host "🎉 Setup complete!" -ForegroundColor Green

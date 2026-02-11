# Complete Lambda Deployment - Build, Upload, and Deploy Infrastructure
param(
    [string]$StackName = "user-registration-lambda",
    [string]$Environment = "production",
    [string]$Region = "us-east-1"
)

Write-Host "User Registration Service - Complete Lambda Deployment" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

# Check AWS CLI
if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    Write-Host "AWS CLI not found!" -ForegroundColor Red
    exit 1
}

Write-Host "AWS CLI found" -ForegroundColor Green

# Check credentials
Write-Host "Checking AWS credentials..." -ForegroundColor Yellow
$identity = aws sts get-caller-identity 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "AWS credentials not configured!" -ForegroundColor Red
    exit 1
}

$accountId = (aws sts get-caller-identity --query Account --output text)
Write-Host "AWS credentials configured" -ForegroundColor Green
Write-Host "Account: $accountId" -ForegroundColor Gray
Write-Host ""

# Get configuration
Write-Host "Step 1: Configuration" -ForegroundColor Yellow
Write-Host "------------------------" -ForegroundColor Yellow
$stackNameInput = Read-Host "CloudFormation stack name [$StackName]"
if ($stackNameInput) { $StackName = $stackNameInput }

$envInput = Read-Host "Environment name [$Environment]"
if ($envInput) { $Environment = $envInput }

$regionInput = Read-Host "AWS region [$Region]"
if ($regionInput) { $Region = $regionInput }

$dbUsername = Read-Host "Database username [postgres]"
if (-not $dbUsername) { $dbUsername = "postgres" }

$dbPassword = Read-Host "Database password (min 8 characters)" -AsSecureString
$dbPasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbPassword)
)

Write-Host ""

# Step 2: Build application
Write-Host "Step 2: Building Application" -ForegroundColor Yellow
Write-Host "------------------------" -ForegroundColor Yellow

$mvnPath = "C:\Users\CAESAR\Downloads\apache-maven-3.9.12-bin\apache-maven-3.9.12\bin\mvn.cmd"

Write-Host "Building JAR file..." -ForegroundColor Gray
& $mvnPath clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Find the shaded JAR file (Maven Shade plugin creates *-aws.jar)
$jarFile = Get-ChildItem -Path "target" -Filter "*-aws.jar" | Select-Object -First 1 -ExpandProperty FullName

if (-not $jarFile) {
    Write-Host "Shaded JAR file not found in target directory!" -ForegroundColor Red
    Write-Host "Looking for files:" -ForegroundColor Yellow
    Get-ChildItem -Path "target" -Filter "*.jar"
    exit 1
}

Write-Host "Build successful!" -ForegroundColor Green
Write-Host "Found JAR: $jarFile" -ForegroundColor Cyan
Write-Host ""

# Step 3: Create S3 bucket for deployment
Write-Host "Step 3: Prepare S3 Bucket" -ForegroundColor Yellow
Write-Host "------------------------" -ForegroundColor Yellow

$bucketName = "$Environment-lambda-deployments-$accountId"
Write-Host "Bucket name: $bucketName" -ForegroundColor Gray

# Check if bucket exists
$bucketExists = aws s3 ls "s3://$bucketName" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating S3 bucket..." -ForegroundColor Gray
    aws s3 mb "s3://$bucketName" --region $Region
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to create bucket!" -ForegroundColor Red
        exit 1
    }
    Write-Host "Bucket created!" -ForegroundColor Green
} else {
    Write-Host "Bucket already exists" -ForegroundColor Green
}

Write-Host ""

# Step 4: Upload JAR to S3
Write-Host "Step 4: Upload Application" -ForegroundColor Yellow
Write-Host "------------------------" -ForegroundColor Yellow

Write-Host "Uploading JAR to S3..." -ForegroundColor Gray
aws s3 cp $jarFile "s3://$bucketName/user-registration.jar" --region $Region

if ($LASTEXITCODE -ne 0) {
    Write-Host "Upload failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Upload successful!" -ForegroundColor Green
Write-Host ""

# Step 5: Deploy CloudFormation stack
Write-Host "Step 5: Deploy Infrastructure" -ForegroundColor Yellow
Write-Host "------------------------" -ForegroundColor Yellow

Write-Host "Deploying CloudFormation stack..." -ForegroundColor Gray
Write-Host "This will take 10-15 minutes..." -ForegroundColor Gray

$output = aws cloudformation create-stack `
    --stack-name $StackName `
    --template-body file://aws/lambda-infrastructure.yml `
    --parameters `
        ParameterKey=EnvironmentName,ParameterValue=$Environment `
        ParameterKey=DatabaseUsername,ParameterValue=$dbUsername `
        ParameterKey=DatabasePassword,ParameterValue=$dbPasswordPlain `
        ParameterKey=DeploymentBucketName,ParameterValue=$bucketName `
    --capabilities CAPABILITY_IAM `
    --region $Region `
    2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "Stack creation failed!" -ForegroundColor Red
    Write-Host $output -ForegroundColor Red
    exit 1
}

Write-Host $output
Write-Host ""
Write-Host "Waiting for stack creation..." -ForegroundColor Gray

aws cloudformation wait stack-create-complete `
    --stack-name $StackName `
    --region $Region

if ($LASTEXITCODE -ne 0) {
    Write-Host "Stack creation failed!" -ForegroundColor Red
    Write-Host "Check AWS Console: https://console.aws.amazon.com/cloudformation/" -ForegroundColor Yellow
    exit 1
}

Write-Host "Stack created successfully!" -ForegroundColor Green
Write-Host ""

# Step 6: Update Lambda with actual code
Write-Host "Step 6: Update Lambda Function" -ForegroundColor Yellow
Write-Host "------------------------" -ForegroundColor Yellow

$functionArn = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`LambdaFunctionArn`].OutputValue' `
    --output text

$functionName = $functionArn.Split(':')[-1]

Write-Host "Updating function: $functionName" -ForegroundColor Gray

aws lambda update-function-code `
    --function-name $functionName `
    --s3-bucket $bucketName `
    --s3-key user-registration.jar `
    --region $Region | Out-Null

Write-Host "Waiting for update to complete..." -ForegroundColor Gray

aws lambda wait function-updated `
    --function-name $functionName `
    --region $Region

Write-Host "Lambda updated!" -ForegroundColor Green
Write-Host ""

# Step 7: Display outputs
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "===================" -ForegroundColor Green
Write-Host ""

$apiEndpoint = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' `
    --output text

$dbEndpoint = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' `
    --output text

Write-Host "API Endpoint: $apiEndpoint" -ForegroundColor Cyan
Write-Host "Database Endpoint: $dbEndpoint" -ForegroundColor Cyan
Write-Host "Deployment Bucket: $bucketName" -ForegroundColor Cyan
Write-Host ""

Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Update application secrets in AWS Secrets Manager" -ForegroundColor White
Write-Host "2. Test the API: curl $apiEndpoint/actuator/health" -ForegroundColor White
Write-Host ""

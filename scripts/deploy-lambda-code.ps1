# Deploy Lambda Application Code
param(
    [string]$StackName = "user-registration-lambda",
    [string]$Region = "us-east-1"
)

Write-Host "User Registration Service - Lambda Code Deployment" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build the application
Write-Host "Step 1: Building application..." -ForegroundColor Yellow
Write-Host ""

$mvnPath = "C:\Users\CAESAR\Downloads\apache-maven-3.9.12-bin\apache-maven-3.9.12\bin\mvn.cmd"

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

# Step 2: Get deployment bucket name from CloudFormation
Write-Host "Step 2: Getting deployment bucket..." -ForegroundColor Yellow

$bucketName = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`DeploymentBucketName`].OutputValue' `
    --output text

if ([string]::IsNullOrEmpty($bucketName)) {
    Write-Host "Could not find deployment bucket!" -ForegroundColor Red
    exit 1
}

Write-Host "Deployment bucket: $bucketName" -ForegroundColor Green
Write-Host ""

# Step 3: Upload JAR to S3
Write-Host "Step 3: Uploading application to S3..." -ForegroundColor Yellow

aws s3 cp $jarFile "s3://$bucketName/user-registration.jar" --region $Region

if ($LASTEXITCODE -ne 0) {
    Write-Host "Upload failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Upload successful!" -ForegroundColor Green
Write-Host ""

# Step 4: Get Lambda function name
Write-Host "Step 4: Updating Lambda function..." -ForegroundColor Yellow

$functionName = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`LambdaFunctionArn`].OutputValue' `
    --output text

$functionName = $functionName.Split(':')[-1]

Write-Host "Function name: $functionName" -ForegroundColor Green

# Step 5: Update Lambda function code
aws lambda update-function-code `
    --function-name $functionName `
    --s3-bucket $bucketName `
    --s3-key user-registration.jar `
    --region $Region | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "Lambda update failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Waiting for Lambda update to complete..." -ForegroundColor Yellow

aws lambda wait function-updated `
    --function-name $functionName `
    --region $Region

Write-Host ""
Write-Host "Deployment complete!" -ForegroundColor Green
Write-Host ""

# Step 6: Get API endpoint
$apiEndpoint = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' `
    --output text

Write-Host "API Endpoint: $apiEndpoint" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test the API:" -ForegroundColor Yellow
Write-Host "  curl $apiEndpoint/actuator/health" -ForegroundColor White
Write-Host ""

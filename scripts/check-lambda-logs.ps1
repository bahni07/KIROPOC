# Check Lambda function logs
param(
    [string]$FunctionName = "production-user-registration",
    [string]$Region = "us-east-1",
    [int]$Lines = 50
)

Write-Host "Fetching Lambda logs..." -ForegroundColor Cyan
Write-Host ""

# Get the log group name
$logGroup = "/aws/lambda/$FunctionName"

# Get the latest log stream
$latestStream = aws logs describe-log-streams `
    --log-group-name $logGroup `
    --order-by LastEventTime `
    --descending `
    --max-items 1 `
    --region $Region `
    --query 'logStreams[0].logStreamName' `
    --output text

if ([string]::IsNullOrEmpty($latestStream)) {
    Write-Host "No log streams found!" -ForegroundColor Red
    exit 1
}

Write-Host "Latest log stream: $latestStream" -ForegroundColor Gray
Write-Host ""

# Get the logs
aws logs get-log-events `
    --log-group-name $logGroup `
    --log-stream-name $latestStream `
    --limit $Lines `
    --region $Region `
    --query 'events[*].message' `
    --output text

Write-Host ""

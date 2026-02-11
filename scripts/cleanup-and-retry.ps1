# Cleanup failed stack and retry deployment
Write-Host "Cleaning up failed CloudFormation stack..." -ForegroundColor Yellow

# Delete the failed stack
aws cloudformation delete-stack --stack-name user-registration-lambda

Write-Host "Waiting for stack deletion to complete..." -ForegroundColor Yellow
Write-Host "This may take a few minutes..." -ForegroundColor Gray

# Wait for deletion
aws cloudformation wait stack-delete-complete --stack-name user-registration-lambda

Write-Host "Stack deleted successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Now you can retry the deployment by running:" -ForegroundColor Cyan
Write-Host "  .\scripts\setup-lambda.ps1" -ForegroundColor White

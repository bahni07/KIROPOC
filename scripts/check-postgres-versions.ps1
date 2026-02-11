# Check available PostgreSQL versions in us-east-1
Write-Host "Checking available PostgreSQL versions in us-east-1..." -ForegroundColor Cyan

aws rds describe-db-engine-versions `
    --engine postgres `
    --region us-east-1 `
    --query 'DBEngineVersions[?contains(EngineVersion, `15.`) || contains(EngineVersion, `16.`)].EngineVersion' `
    --output table

Write-Host ""
Write-Host "Latest versions:" -ForegroundColor Yellow
aws rds describe-db-engine-versions `
    --engine postgres `
    --region us-east-1 `
    --query 'DBEngineVersions[-5:].EngineVersion' `
    --output table

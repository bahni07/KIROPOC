# Update AWS Secrets Manager with application secrets
param(
    [string]$Environment = "production",
    [string]$Region = "us-east-1"
)

Write-Host "Update Application Secrets" -ForegroundColor Cyan
Write-Host "===========================" -ForegroundColor Cyan
Write-Host ""

# Get the database password from .env file
$dbPassword = ""
if (Test-Path ".env") {
    $envContent = Get-Content ".env"
    foreach ($line in $envContent) {
        if ($line -match "^SPRING_DATASOURCE_PASSWORD=(.+)$") {
            $dbPassword = $matches[1]
            break
        }
    }
}

if (-not $dbPassword) {
    $dbPasswordSecure = Read-Host "Database password" -AsSecureString
    $dbPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbPasswordSecure)
    )
}

Write-Host "Enter application secrets (press Enter to skip):" -ForegroundColor Yellow
Write-Host ""

$emailUsername = Read-Host "Email username (Gmail address)"
if (-not $emailUsername) { $emailUsername = "your_email@gmail.com" }

$emailPassword = Read-Host "Email app password" -AsSecureString
$emailPasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($emailPassword)
)
if (-not $emailPasswordPlain) { $emailPasswordPlain = "your_app_password" }

$googleClientId = Read-Host "Google OAuth Client ID"
if (-not $googleClientId) { $googleClientId = "your_google_client_id" }

$googleClientSecret = Read-Host "Google OAuth Client Secret" -AsSecureString
$googleClientSecretPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($googleClientSecret)
)
if (-not $googleClientSecretPlain) { $googleClientSecretPlain = "your_google_client_secret" }

$amazonClientId = Read-Host "Amazon OAuth Client ID"
if (-not $amazonClientId) { $amazonClientId = "your_amazon_client_id" }

$amazonClientSecret = Read-Host "Amazon OAuth Client Secret" -AsSecureString
$amazonClientSecretPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($amazonClientSecret)
)
if (-not $amazonClientSecretPlain) { $amazonClientSecretPlain = "your_amazon_client_secret" }

# Get encryption key from .env
$encryptionKey = ""
if (Test-Path ".env") {
    $envContent = Get-Content ".env"
    foreach ($line in $envContent) {
        if ($line -match "^ENCRYPTION_KEY=(.+)$") {
            $encryptionKey = $matches[1]
            break
        }
    }
}

if (-not $encryptionKey) {
    Write-Host "Generating new encryption key..." -ForegroundColor Yellow
    $bytes = New-Object byte[] 32
    [Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
    $encryptionKey = [Convert]::ToBase64String($bytes)
}

Write-Host ""
Write-Host "Updating secrets in AWS Secrets Manager..." -ForegroundColor Yellow

$secretJson = @{
    SPRING_DATASOURCE_PASSWORD = $dbPassword
    EMAIL_USERNAME = $emailUsername
    EMAIL_PASSWORD = $emailPasswordPlain
    GOOGLE_CLIENT_ID = $googleClientId
    GOOGLE_CLIENT_SECRET = $googleClientSecretPlain
    AMAZON_CLIENT_ID = $amazonClientId
    AMAZON_CLIENT_SECRET = $amazonClientSecretPlain
    ENCRYPTION_KEY = $encryptionKey
} | ConvertTo-Json -Compress

$secretName = "$Environment/lambda/user-registration/secrets"

aws secretsmanager update-secret `
    --secret-id $secretName `
    --secret-string $secretJson `
    --region $Region | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "Secrets updated successfully!" -ForegroundColor Green
} else {
    Write-Host "Failed to update secrets!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Note: Lambda needs to be configured to read these secrets." -ForegroundColor Yellow
Write-Host "The current Lambda uses environment variables instead." -ForegroundColor Yellow
Write-Host ""
